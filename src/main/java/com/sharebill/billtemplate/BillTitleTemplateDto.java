package com.sharebill.billtemplate;

public record BillTitleTemplateDto(
    String id,
    String label,
    int position
) {
  public static BillTitleTemplateDto from(BillTitleTemplateEntity entity) {
    return new BillTitleTemplateDto(entity.getId(), entity.getLabel(), entity.getPosition());
  }
}
