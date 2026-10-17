output "frontend_url" { value = "https://${aws_cloudfront_distribution.web.domain_name}" }
output "frontend_bucket" { value = aws_s3_bucket.web.id }
output "distribution_id" { value = aws_cloudfront_distribution.web.id }
