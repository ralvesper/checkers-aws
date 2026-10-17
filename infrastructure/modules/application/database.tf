resource "aws_db_subnet_group" "main" {
  name       = var.name
  subnet_ids = var.data_subnets
}
resource "aws_db_instance" "main" {
  identifier                  = var.name
  engine                      = "postgres"
  engine_version              = "17"
  instance_class              = var.db_instance_class
  allocated_storage           = 20
  max_allocated_storage       = 40
  storage_type                = "gp3"
  storage_encrypted           = true
  db_name                     = "checkers"
  username                    = "checkers"
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.main.name
  vpc_security_group_ids      = [aws_security_group.db.id]
  publicly_accessible         = false
  multi_az                    = var.db_multi_az
  backup_retention_period     = 7
  deletion_protection         = var.db_deletion_protection
  skip_final_snapshot         = false
  final_snapshot_identifier   = var.final_snapshot_identifier
  auto_minor_version_upgrade  = true
  copy_tags_to_snapshot       = true
}
