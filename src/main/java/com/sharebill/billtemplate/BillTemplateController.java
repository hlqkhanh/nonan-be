package com.sharebill.billtemplate;

import com.sharebill.user.UserEntity;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/bill-templates")
public class BillTemplateController {
  private final BillTemplateService billTemplateService;

  public BillTemplateController(BillTemplateService billTemplateService) {
    this.billTemplateService = billTemplateService;
  }

  @GetMapping
  public List<BillTitleTemplateDto> list(@AuthenticationPrincipal UserEntity user) {
    return billTemplateService.list(user.getId());
  }

  @PutMapping
  public List<BillTitleTemplateDto> replaceAll(@RequestBody SaveBillTemplatesRequest request,
      @AuthenticationPrincipal UserEntity user) {
    return billTemplateService.replaceAll(user.getId(), request);
  }
}
