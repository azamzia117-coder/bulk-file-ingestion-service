# Landing bucket for inbound files, locked down by default, plus a least-privilege read policy
# for the ingestion service's IAM role. Values come from variables; nothing account-specific is committed.

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

resource "aws_s3_bucket" "landing" {
  bucket = var.bucket_name
  tags   = var.tags
}

resource "aws_s3_bucket_public_access_block" "landing" {
  bucket                  = aws_s3_bucket.landing.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "landing" {
  bucket = aws_s3_bucket.landing.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "landing" {
  bucket = aws_s3_bucket.landing.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "landing" {
  bucket = aws_s3_bucket.landing.id
  rule {
    id     = "expire-processed-files"
    status = "Enabled"
    filter {
      prefix = "processed/"
    }
    expiration {
      days = var.processed_retention_days
    }
  }
}

data "aws_iam_policy_document" "read_landing" {
  statement {
    sid       = "ReadInboundFiles"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.landing.arn}/inbound/*"]
  }
  statement {
    sid       = "ListInboundPrefix"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.landing.arn]
    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["inbound/*"]
    }
  }
}

resource "aws_iam_policy" "read_landing" {
  name   = "${var.bucket_name}-ingestion-read"
  policy = data.aws_iam_policy_document.read_landing.json
}
