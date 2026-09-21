data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "platform_key" {
  statement {
    sid       = "AccountAdministration"
    effect    = "Allow"
    actions   = ["kms:*"]
    resources = ["*"]

    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"]
    }
  }

  statement {
    sid    = "CloudWatchLogsEncryption"
    effect = "Allow"
    actions = [
      "kms:Decrypt",
      "kms:DescribeKey",
      "kms:Encrypt",
      "kms:GenerateDataKey*",
      "kms:ReEncrypt*",
    ]
    resources = ["*"]

    principals {
      type        = "Service"
      identifiers = ["logs.${var.aws_region}.amazonaws.com"]
    }

    condition {
      test     = "ArnLike"
      variable = "kms:EncryptionContext:aws:logs:arn"
      values = [
        "arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:log-group:/insurance/${var.environment}/*",
      ]
    }
  }
}

locals {
  name = "insurance-${var.environment}"
  repositories = toset([
    "case-service",
    "integration-service",
    "workflow-service",
    "web-bff",
    "operations-web",
    "automation-worker",
  ])
}

resource "aws_vpc" "platform" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${local.name}-vpc"
  }
}

resource "aws_subnet" "data" {
  count = 2

  vpc_id                  = aws_vpc.platform.id
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  cidr_block              = cidrsubnet(var.vpc_cidr, 4, count.index)
  map_public_ip_on_launch = false

  tags = {
    Name = "${local.name}-data-${count.index + 1}"
  }
}

resource "aws_db_subnet_group" "platform" {
  name       = "${local.name}-database"
  subnet_ids = aws_subnet.data[*].id
}

resource "aws_security_group" "database" {
  name        = "${local.name}-database"
  description = "PostgreSQL access from the OpenShift workload network"
  vpc_id      = aws_vpc.platform.id

  ingress {
    description = "PostgreSQL from OpenShift"
    protocol    = "tcp"
    from_port   = 5432
    to_port     = 5432
    cidr_blocks = [var.openshift_workload_cidr]
  }

  egress {
    protocol    = "-1"
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_kms_key" "platform" {
  description             = "Insurance platform data encryption"
  enable_key_rotation     = true
  deletion_window_in_days = 30
  policy                  = data.aws_iam_policy_document.platform_key.json
}

resource "aws_kms_alias" "platform" {
  name          = "alias/${local.name}"
  target_key_id = aws_kms_key.platform.key_id
}

resource "aws_db_instance" "platform" {
  identifier = "${local.name}-postgres"

  engine         = "postgres"
  engine_version = var.postgres_engine_version
  instance_class = var.postgres_instance_class

  allocated_storage     = 50
  max_allocated_storage = 250
  storage_type          = "gp3"
  storage_encrypted     = true
  kms_key_id            = aws_kms_key.platform.arn

  db_name                       = "insurance_platform"
  username                      = "insurance_admin"
  manage_master_user_password   = true
  master_user_secret_kms_key_id = aws_kms_key.platform.arn

  db_subnet_group_name   = aws_db_subnet_group.platform.name
  vpc_security_group_ids = [aws_security_group.database.id]
  publicly_accessible    = false
  multi_az               = true

  backup_retention_period   = 14
  copy_tags_to_snapshot     = true
  deletion_protection       = true
  skip_final_snapshot       = false
  final_snapshot_identifier = "${local.name}-postgres-final"

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "evidence" {
  bucket_prefix = "${local.name}-evidence-"
}

resource "aws_s3_bucket_public_access_block" "evidence" {
  bucket = aws_s3_bucket.evidence.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

data "aws_iam_policy_document" "evidence" {
  statement {
    sid       = "DenyInsecureTransport"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = [aws_s3_bucket.evidence.arn, "${aws_s3_bucket.evidence.arn}/*"]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "evidence" {
  bucket = aws_s3_bucket.evidence.id
  policy = data.aws_iam_policy_document.evidence.json
}

resource "aws_s3_bucket_versioning" "evidence" {
  bucket = aws_s3_bucket.evidence.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "evidence" {
  bucket = aws_s3_bucket.evidence.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.platform.arn
      sse_algorithm     = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "evidence" {
  bucket = aws_s3_bucket.evidence.id

  rule {
    id     = "expire-noncurrent-versions"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = var.evidence_retention_days
    }
  }
}

resource "aws_ecr_repository" "service" {
  for_each = local.repositories

  name                 = "${local.name}/${each.key}"
  image_tag_mutability = "IMMUTABLE"

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = aws_kms_key.platform.arn
  }

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "service" {
  for_each = aws_ecr_repository.service

  repository = each.value.name
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Retain the most recent 50 release images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 50
        }
        action = {
          type = "expire"
        }
      },
    ]
  })
}

resource "aws_secretsmanager_secret" "application" {
  for_each = toset([
    "oidc-clients",
    "payment-callback",
    "legacy-database",
  ])

  name                    = "${local.name}/${each.key}"
  kms_key_id              = aws_kms_key.platform.arn
  recovery_window_in_days = 30
}

resource "aws_cloudwatch_log_group" "application" {
  for_each = local.repositories

  name              = "/insurance/${var.environment}/${each.key}"
  retention_in_days = 30
  kms_key_id        = aws_kms_key.platform.arn
}
