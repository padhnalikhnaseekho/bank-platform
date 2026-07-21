resource "aws_s3_bucket" "this" {
  for_each = var.buckets

  bucket = "${var.name_prefix}-${each.key}"

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-${each.key}"
  })
}

# Block all public access on every bucket — statements/reports/audit archives are
# accessed exclusively through service task roles, never directly by the public.
resource "aws_s3_bucket_public_access_block" "this" {
  for_each = var.buckets

  bucket = aws_s3_bucket.this[each.key].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  for_each = var.buckets

  bucket = aws_s3_bucket.this[each.key].id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_versioning" "this" {
  for_each = var.buckets

  bucket = aws_s3_bucket.this[each.key].id

  versioning_configuration {
    status = each.value.versioning_enabled ? "Enabled" : "Suspended"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  for_each = { for k, v in var.buckets : k => v if v.glacier_transition_days != null || v.noncurrent_expiration_days != null }

  bucket = aws_s3_bucket.this[each.key].id

  rule {
    id     = "archival"
    status = "Enabled"

    dynamic "transition" {
      for_each = each.value.glacier_transition_days != null ? [each.value.glacier_transition_days] : []
      content {
        days          = transition.value
        storage_class = "GLACIER"
      }
    }

    dynamic "noncurrent_version_expiration" {
      for_each = each.value.noncurrent_expiration_days != null ? [each.value.noncurrent_expiration_days] : []
      content {
        noncurrent_days = noncurrent_version_expiration.value
      }
    }
  }

  depends_on = [aws_s3_bucket_versioning.this]
}
