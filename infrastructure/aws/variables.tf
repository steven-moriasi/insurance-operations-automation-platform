variable "aws_region" {
  description = "AWS region for managed dependencies."
  type        = string
  default     = "af-south-1"
}

variable "environment" {
  description = "Environment name used in resource names and tags."
  type        = string
  default     = "reference"
}

variable "vpc_cidr" {
  description = "CIDR for the dependency VPC."
  type        = string
  default     = "10.42.0.0/16"
}

variable "openshift_workload_cidr" {
  description = "CIDR allowed to connect to PostgreSQL. Replace before deployment."
  type        = string
}

variable "postgres_engine_version" {
  description = "Approved PostgreSQL engine version."
  type        = string
  default     = "17.6"
}

variable "postgres_instance_class" {
  description = "RDS instance class for the reference target."
  type        = string
  default     = "db.t4g.medium"
}

variable "evidence_retention_days" {
  description = "Days before noncurrent evidence object versions expire."
  type        = number
  default     = 365
}
