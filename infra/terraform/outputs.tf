output "bucket_arn" {
  value = aws_s3_bucket.landing.arn
}

output "read_policy_arn" {
  value = aws_iam_policy.read_landing.arn
}
