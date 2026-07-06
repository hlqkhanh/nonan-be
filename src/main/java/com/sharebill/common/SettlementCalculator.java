package com.sharebill.common;

import com.sharebill.expense.ExpenseDto;
import com.sharebill.expense.ParticipantShareDto;
import com.sharebill.expense.PayerContributionDto;
import com.sharebill.settlement.SettlementDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SettlementCalculator {
  private SettlementCalculator() {
  }

  public static List<SettlementDto> calculate(List<ExpenseDto> expenses, Set<String> paidSettlementIds) {
    Map<String, Long> netByMember = new LinkedHashMap<>();

    for (ExpenseDto expense : expenses) {
      for (PayerContributionDto payer : expense.payers()) {
        netByMember.merge(payer.memberId(), payer.amount(), Long::sum);
      }

      for (ParticipantShareDto participant : expense.participants()) {
        netByMember.merge(participant.memberId(), -participant.amount(), Long::sum);
      }
    }

    List<Balance> debtors = netByMember.entrySet().stream()
        .filter(entry -> entry.getValue() < 0)
        .map(entry -> new Balance(entry.getKey(), -entry.getValue()))
        .toList();
    List<Balance> creditors = netByMember.entrySet().stream()
        .filter(entry -> entry.getValue() > 0)
        .map(entry -> new Balance(entry.getKey(), entry.getValue()))
        .toList();

    List<SettlementDto> settlements = new ArrayList<>();
    int debtorIndex = 0;
    int creditorIndex = 0;

    while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
      Balance debtor = debtors.get(debtorIndex);
      Balance creditor = creditors.get(creditorIndex);
      long amount = Math.min(debtor.amount, creditor.amount);

      if (amount > 0) {
        String id = debtor.memberId + "->" + creditor.memberId;
        settlements.add(new SettlementDto(id, debtor.memberId, creditor.memberId, amount, paidSettlementIds.contains(id)));
      }

      debtor.amount -= amount;
      creditor.amount -= amount;

      if (debtor.amount == 0) {
        debtorIndex++;
      }
      if (creditor.amount == 0) {
        creditorIndex++;
      }
    }

    return settlements;
  }

  private static final class Balance {
    private final String memberId;
    private long amount;

    private Balance(String memberId, long amount) {
      this.memberId = memberId;
      this.amount = amount;
    }
  }
}
