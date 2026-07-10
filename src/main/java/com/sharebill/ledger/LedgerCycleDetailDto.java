package com.sharebill.ledger;

import com.sharebill.audit.AuditLogDto;
import com.sharebill.expense.ExpenseDto;
import java.util.List;
import java.util.Map;

public record LedgerCycleDetailDto(
    LedgerCycleDto cycle,
    List<ExpenseDto> expenses,
    List<SettlementSnapshotDto> settlements,
    List<AuditLogDto> auditLogs,
    Map<String, LedgerMemberInfoDto> members
) {
}
