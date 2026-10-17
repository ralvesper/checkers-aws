mock_provider "aws" {
  mock_data "aws_availability_zones" {
    defaults = { names = ["us-east-1a", "us-east-1b"] }
  }
  mock_data "aws_caller_identity" {
    defaults = { account_id = "123456789012" }
  }
  mock_resource "aws_db_instance" {
    defaults = {
      master_user_secret = [{
        secret_arn    = "arn:aws:secretsmanager:us-east-1:123456789012:secret:test-123456"
        secret_status = "active"
        kms_key_id    = "arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012"
      }]
    }
  }
}
mock_provider "aws" { alias = "edge" }
mock_provider "random" {}

run "network_only_by_default" {
  command = plan
  assert {
    condition     = length(aws_subnet.public) == 2 && length(aws_subnet.application) == 2 && length(aws_subnet.data) == 2
    error_message = "The lab must have public, app and data subnets in two AZs."
  }
  assert {
    condition     = length(module.application) == 0 && length(aws_nat_gateway.main) == 0 && length(aws_ecr_repository.backend) == 0
    error_message = "The default network stage must not create application, NAT or registry resources."
  }
}

run "application_plan_without_aws_access" {
  command = plan
  variables {
    enable_application = true
    backend_image      = "123456789012.dkr.ecr.us-east-1.amazonaws.com/checkers-lab-backend:v1"
    api_hostname       = "api.example.com"
    route53_zone_id    = "Z123456789EXAMPLE"
    certificate_arn    = "arn:aws:acm:us-east-1:123456789012:certificate/12345678-1234-1234-1234-123456789012"
    trusted_ipv4_cidrs = ["203.0.113.10/32"]
  }
  assert {
    condition     = length(module.application) == 1 && length(aws_nat_gateway.main) == 1
    error_message = "The explicit application stage requires one application module and outbound NAT."
  }
}
