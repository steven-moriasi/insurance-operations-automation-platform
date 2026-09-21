output "vpc_id" {
  value = aws_vpc.platform.id
}

output "postgres_endpoint" {
  value = aws_db_instance.platform.address
}

output "postgres_master_secret_arn" {
  value     = aws_db_instance.platform.master_user_secret[0].secret_arn
  sensitive = true
}

output "evidence_bucket" {
  value = aws_s3_bucket.evidence.id
}

output "ecr_repository_urls" {
  value = {
    for name, repository in aws_ecr_repository.service : name => repository.repository_url
  }
}

output "application_secret_arns" {
  value = {
    for name, secret in aws_secretsmanager_secret.application : name => secret.arn
  }
}
