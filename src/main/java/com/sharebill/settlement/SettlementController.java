package com.sharebill.settlement;

import com.sharebill.group.GroupAccessService;
import com.sharebill.ledger.LedgerCycleEntity;
import com.sharebill.ledger.LedgerService;
import com.sharebill.user.UserEntity;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
public class SettlementController {
  private final LedgerService ledgerService;
  private final GroupAccessService groupAccessService;

  public SettlementController(LedgerService ledgerService, GroupAccessService groupAccessService) {
    this.ledgerService = ledgerService;
    this.groupAccessService = groupAccessService;
  }

  @GetMapping
  public List<SettlementDto> settlements(@PathVariable String groupId, @AuthenticationPrincipal UserEntity user) {
    groupAccessService.requireMember(groupId, user.getId());
    LedgerCycleEntity cycle = ledgerService.ensureOpenCycle(groupId);
    return ledgerService.calculateCycleSettlements(cycle.getId());
  }
}
