package com.sharebill.settlement;

import com.sharebill.common.ShareBillService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
public class SettlementController {
  private final ShareBillService service;

  public SettlementController(ShareBillService service) {
    this.service = service;
  }

  @GetMapping
  public List<SettlementDto> settlements(@PathVariable String groupId) {
    return service.settlements(groupId);
  }

  @PostMapping("/{settlementId}/mark-paid")
  public List<SettlementDto> markPaid(@PathVariable String groupId, @PathVariable String settlementId) {
    return service.markSettlementPaid(groupId, settlementId);
  }
}
