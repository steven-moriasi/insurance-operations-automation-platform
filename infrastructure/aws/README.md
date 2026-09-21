# AWS Managed-Dependency Target

This Terraform package defines a reviewable AWS target for services normally consumed by the OpenShift workloads:

- isolated networking and private subnets;
- encrypted Multi-AZ PostgreSQL with AWS-managed master credentials and backups;
- private, versioned, KMS-encrypted evidence storage;
- immutable and scan-on-push ECR repositories;
- Secrets Manager containers for integration credentials;
- encrypted CloudWatch log groups.

It deliberately does not insert secret values, create a production identity realm, or claim a live deployment. The case and integration schemas must use separate database users and logical databases created by a controlled bootstrap job.

```bash
cp infrastructure/aws/terraform.tfvars.example infrastructure/aws/terraform.tfvars
terraform -chdir=infrastructure/aws init
terraform -chdir=infrastructure/aws fmt -check
terraform -chdir=infrastructure/aws validate
terraform -chdir=infrastructure/aws plan
```

Review CIDRs, engine availability, deletion protection, backup retention, KMS policy, data residency, and workload identity with the adopting insurer before applying.
