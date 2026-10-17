variable "region" {
  type    = string
  default = "us-east-1"
}
variable "name" {
  type    = string
  default = "checkers-lab"
  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,23}$", var.name))
    error_message = "Use 3 to 24 lowercase letters, digits or hyphens, starting with a letter."
  }
}
variable "vpc_cidr" {
  type    = string
  default = "10.42.0.0/16"
  validation {
    condition     = can(cidrsubnet(var.vpc_cidr, 8, 21))
    error_message = "Use an IPv4 CIDR with space for the six /8-extension subnets."
  }
}
variable "enable_registry" {
  type    = bool
  default = false
}
variable "enable_application" {
  type    = bool
  default = false
}
variable "backend_image" {
  type    = string
  default = ""
}
variable "api_hostname" {
  type    = string
  default = ""
}
variable "route53_zone_id" {
  type    = string
  default = ""
}
variable "certificate_arn" {
  description = "Existing validated ACM certificate in the application region, covering api_hostname."
  type        = string
  default     = ""
}
variable "trusted_ipv4_cidrs" {
  description = "Viewer IPs allowed by CloudFront WAF for this lab; never the whole internet."
  type        = list(string)
  default     = []
  validation {
    condition     = alltrue([for cidr in var.trusted_ipv4_cidrs : can(cidrnetmask(cidr)) && cidr != "0.0.0.0/0"])
    error_message = "Supply valid, restricted IPv4 CIDRs; 0.0.0.0/0 is not allowed for the prototype."
  }
}
variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}
variable "db_multi_az" {
  type    = bool
  default = false
}
variable "db_deletion_protection" {
  type    = bool
  default = true
}
variable "final_snapshot_identifier" {
  type    = string
  default = "checkers-lab-final"
}
