package com.sharebill.settlement;

import com.sharebill.ledger.LedgerCycleEntity;
import com.sharebill.ledger.LedgerService;
import com.sharebill.user.UserEntity;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {
  private final LedgerService ledgerService;

  public SettlementController(LedgerService ledgerService) {
    this.ledgerService = ledgerService;
  }

  @GetMapping
  public List<SettlementDto> settlements(@AuthenticationPrincipal UserEntity user) {
    LedgerCycleEntity cycle = ledgerService.ensureOpenCycle(user.getId());
    return ledgerService.calculateCycleSettlements(cycle.getId());
  }
}
