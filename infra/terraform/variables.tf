variable "region" {
  type    = string
  default = "us-east-1"
}

variable "bucket_name" {
  type        = string
  description = "Globally unique name for the landing bucket"
}

variable "processed_retention_days" {
  type    = number
  default = 30
}

variable "tags" {
  type    = map(string)
  default = { project = "bulk-file-ingestion-service" }
}
