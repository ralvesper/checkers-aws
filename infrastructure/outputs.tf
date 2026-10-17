output "vpc_id" { value = aws_vpc.main.id }
output "public_subnets" { value = aws_subnet.public[*].id }
output "application_subnets" { value = aws_subnet.application[*].id }
output "data_subnets" { value = aws_subnet.data[*].id }
output "ecr_repository" { value = try(aws_ecr_repository.backend[0].repository_url, null) }
output "frontend_url" { value = try(module.application[0].frontend_url, null) }
output "frontend_bucket" { value = try(module.application[0].frontend_bucket, null) }
output "cloudfront_distribution_id" { value = try(module.application[0].distribution_id, null) }
