resource "aws_sns_topic" "this" {
  for_each = toset(var.topic_names)

  name              = "${var.name_prefix}-${each.value}"
  kms_master_key_id = "alias/aws/sns"

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-${each.value}"
  })
}
