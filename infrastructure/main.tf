resource "terraform_data" "application_inputs" {
  lifecycle {
    precondition {
      condition = !var.enable_application || (
        length(var.backend_image) > 0 && length(var.api_hostname) > 0 &&
        length(var.route53_zone_id) > 0 && length(var.certificate_arn) > 0 &&
        length(var.trusted_ipv4_cidrs) > 0
      )
      error_message = "Application requires image, origin hostname, Route53 zone, ACM certificate and restricted viewer CIDRs."
    }
  }
}

resource "aws_ecr_repository" "backend" {
  count                = var.enable_registry || var.enable_application ? 1 : 0
  name                 = "${var.name}-backend"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration { scan_on_push = true }
  encryption_configuration { encryption_type = "AES256" }
}

module "application" {
  count     = var.enable_application ? 1 : 0
  source    = "./modules/application"
  providers = { aws = aws, aws.edge = aws.edge }

  name                      = var.name
  region                    = var.region
  vpc_id                    = aws_vpc.main.id
  public_subnets            = aws_subnet.public[*].id
  application_subnets       = aws_subnet.application[*].id
  data_subnets              = aws_subnet.data[*].id
  backend_image             = var.backend_image
  api_hostname              = var.api_hostname
  route53_zone_id           = var.route53_zone_id
  certificate_arn           = var.certificate_arn
  trusted_ipv4_cidrs        = var.trusted_ipv4_cidrs
  db_instance_class         = var.db_instance_class
  db_multi_az               = var.db_multi_az
  db_deletion_protection    = var.db_deletion_protection
  final_snapshot_identifier = var.final_snapshot_identifier
  depends_on                = [terraform_data.application_inputs, aws_route.application_outbound]
}
