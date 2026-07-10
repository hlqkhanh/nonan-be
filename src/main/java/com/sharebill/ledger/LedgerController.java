package com.sharebill.ledger;

import com.sharebill.settlement.AdjustRequest;
import com.sharebill.settlement.MarkPaidRequest;
import com.sharebill.settlement.SettlementDto;
import com.sharebill.user.UserEntity;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
  private final LedgerService ledgerService;

  public LedgerController(LedgerService ledgerService) {
    this.ledgerService = ledgerService;
  }

  @GetMapping("/current")
  public LedgerCycleDetailDto current(@AuthenticationPrincipal UserEntity user) {
    return ledgerService.getCurrentCycleDetail(user.getId());
  }

  @GetMapping("/cycles")
  public List<LedgerCycleDto> cycles(@AuthenticationPrincipal UserEntity user) {
    return ledgerService.listCycles(user.getId());
  }

  @GetMapping("/cycles/{cycleId}")
  public LedgerCycleDetailDto cycleDetail(@PathVariable String cycleId, @AuthenticationPrincipal UserEntity user) {
    return ledgerService.getCycleDetail(user.getId(), cycleId);
  }

  @PostMapping("/cycles/{cycleId}/settle")
  public LedgerCycleDetailDto settle(@PathVariable String cycleId, @AuthenticationPrincipal UserEntity user) {
    return ledgerService.closeCycle(user.getId(), cycleId, "settled");
  }

  @PostMapping("/cycles/{cycleId}/archive")
  public LedgerCycleDetailDto archive(@PathVariable String cycleId, @AuthenticationPrincipal UserEntity user) {
    return ledgerService.closeCycle(user.getId(), cycleId, "archived_unpaid");
  }

  @PostMapping("/cycles/{cycleId}/reopen")
  public LedgerCycleDetailDto reopen(@PathVariable String cycleId, @AuthenticationPrincipal UserEntity user) {
    return ledgerService.reopenCycle(user.getId(), cycleId);
  }

  @PostMapping("/cycles/{cycleId}/set-active")
  public LedgerCycleDetailDto setActive(@PathVariable String cycleId, @AuthenticationPrincipal UserEntity user) {
    return ledgerService.setActive(user.getId(), cycleId);
  }

  @PostMapping("/cycles/{cycleId}/settlements/mark-paid")
  public List<SettlementDto> markPaid(@PathVariable String cycleId, @Valid @RequestBody MarkPaidRequest request,
      @AuthenticationPrincipal UserEntity user) {
    return ledgerService.markPaid(user.getId(), cycleId, request.settlementId());
  }

  @PostMapping("/cycles/{cycleId}/settlements/adjust")
  public List<SettlementDto> adjust(@PathVariable String cycleId, @Valid @RequestBody AdjustRequest request,
      @AuthenticationPrincipal UserEntity user) {
    return ledgerService.adjustSettlement(user.getId(), cycleId, request.settlementId(), request.deltaAmount());
  }
}
