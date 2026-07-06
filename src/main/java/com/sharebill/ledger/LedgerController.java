package com.sharebill.ledger;

import com.sharebill.group.GroupAccessService;
import com.sharebill.group.MemberEntity;
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
@RequestMapping("/api/groups/{groupId}/ledger")
public class LedgerController {
  private final LedgerService ledgerService;
  private final GroupAccessService groupAccessService;

  public LedgerController(LedgerService ledgerService, GroupAccessService groupAccessService) {
    this.ledgerService = ledgerService;
    this.groupAccessService = groupAccessService;
  }

  @GetMapping("/current")
  public LedgerCycleDetailDto current(@PathVariable String groupId, @AuthenticationPrincipal UserEntity user) {
    groupAccessService.requireMember(groupId, user.getId());
    return ledgerService.getCurrentCycleDetail(groupId);
  }

  @GetMapping("/cycles")
  public List<LedgerCycleDto> cycles(@PathVariable String groupId, @AuthenticationPrincipal UserEntity user) {
    groupAccessService.requireMember(groupId, user.getId());
    return ledgerService.listCycles(groupId);
  }

  @GetMapping("/cycles/{cycleId}")
  public LedgerCycleDetailDto cycleDetail(@PathVariable String groupId, @PathVariable String cycleId,
      @AuthenticationPrincipal UserEntity user) {
    groupAccessService.requireMember(groupId, user.getId());
    return ledgerService.getCycleDetail(groupId, cycleId);
  }

  @PostMapping("/current/settle")
  public LedgerCycleDetailDto settle(@PathVariable String groupId, @AuthenticationPrincipal UserEntity user) {
    MemberEntity actor = groupAccessService.requireMember(groupId, user.getId());
    return ledgerService.closeCycle(groupId, actor.getId(), "settled");
  }

  @PostMapping("/current/archive")
  public LedgerCycleDetailDto archive(@PathVariable String groupId, @AuthenticationPrincipal UserEntity user) {
    MemberEntity actor = groupAccessService.requireMember(groupId, user.getId());
    return ledgerService.closeCycle(groupId, actor.getId(), "archived_unpaid");
  }

  @PostMapping("/cycles/{cycleId}/settlements/mark-paid")
  public List<SettlementDto> markPaid(@PathVariable String groupId, @PathVariable String cycleId,
      @Valid @RequestBody MarkPaidRequest request, @AuthenticationPrincipal UserEntity user) {
    MemberEntity actor = groupAccessService.requireMember(groupId, user.getId());
    return ledgerService.markPaid(groupId, cycleId, request.settlementId(), actor.getId());
  }

  @PostMapping("/cycles/{cycleId}/settlements/adjust")
  public List<SettlementDto> adjust(@PathVariable String groupId, @PathVariable String cycleId,
      @Valid @RequestBody AdjustRequest request, @AuthenticationPrincipal UserEntity user) {
    MemberEntity actor = groupAccessService.requireMember(groupId, user.getId());
    return ledgerService.adjustSettlement(groupId, cycleId, request.settlementId(), request.deltaAmount(), actor.getId());
  }
}
