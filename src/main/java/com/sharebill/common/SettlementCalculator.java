package com.sharebill.common;

import com.sharebill.expense.ExpenseDto;
import com.sharebill.expense.ParticipantShareDto;
import com.sharebill.expense.PayerContributionDto;
import com.sharebill.settlement.SettlementDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class SettlementCalculator {
  private SettlementCalculator() {
  }

  public static List<SettlementDto> calculate(List<ExpenseDto> expenses, Set<String> paidSettlementIds) {
    Map<String, Long> pairDebt = new LinkedHashMap<>();

    for (ExpenseDto expense : expenses) {
      accumulateExpense(expense, pairDebt);
    }

    Set<String> members = new TreeSet<>();
    for (String key : pairDebt.keySet()) {
      int arrow = key.indexOf("->");
      members.add(key.substring(0, arrow));
      members.add(key.substring(arrow + 2));
    }
    List<String> sortedMembers = new ArrayList<>(members);

    List<Debt> debts = new ArrayList<>();
    for (int i = 0; i < sortedMembers.size(); i++) {
      for (int j = i + 1; j < sortedMembers.size(); j++) {
        String x = sortedMembers.get(i);
        String y = sortedMembers.get(j);
        long xToY = pairDebt.getOrDefault(x + "->" + y, 0L);
        long yToX = pairDebt.getOrDefault(y + "->" + x, 0L);
        long net = xToY - yToX;

        if (net > 0) {
          debts.add(new Debt(x, y, net));
        } else if (net < 0) {
          debts.add(new Debt(y, x, -net));
        }
      }
    }

    debts.sort(Comparator.<Debt, String>comparing(debt -> debt.from).thenComparing(debt -> debt.to));

    List<SettlementDto> settlements = new ArrayList<>(debts.size());
    for (Debt debt : debts) {
      String id = debt.from + "->" + debt.to;
      settlements.add(new SettlementDto(id, debt.from, debt.to, debt.amount, paidSettlementIds.contains(id)));
    }
    return settlements;
  }

  private static void accumulateExpense(ExpenseDto expense, Map<String, Long> pairDebt) {
    Map<String, Long> netByMember = new LinkedHashMap<>();

    for (PayerContributionDto payer : expense.payers()) {
      netByMember.merge(payer.memberId(), payer.amount(), Long::sum);
    }
    for (ParticipantShareDto participant : expense.participants()) {
      netByMember.merge(participant.memberId(), -participant.amount(), Long::sum);
    }

    List<Balance> creditors = new ArrayList<>();
    List<Balance> debtors = new ArrayList<>();
    for (Map.Entry<String, Long> entry : netByMember.entrySet()) {
      if (entry.getValue() > 0) {
        creditors.add(new Balance(entry.getKey(), entry.getValue()));
      } else if (entry.getValue() < 0) {
        debtors.add(new Balance(entry.getKey(), -entry.getValue()));
      }
    }

    creditors.sort(Comparator.comparingLong((Balance balance) -> balance.amount).reversed()
        .thenComparing(balance -> balance.memberId));
    debtors.sort(Comparator.comparing(balance -> balance.memberId));

    long totalSurplus = 0;
    for (Balance creditor : creditors) {
      totalSurplus += creditor.amount;
    }
    if (totalSurplus <= 0) {
      return;
    }

    for (Balance debtor : debtors) {
      long debt = debtor.amount;
      long[] shares = new long[creditors.size()];
      long sumShares = 0;
      for (int i = 0; i < creditors.size(); i++) {
        shares[i] = debt * creditors.get(i).amount / totalSurplus;
        sumShares += shares[i];
      }
      shares[0] += debt - sumShares;

      for (int i = 0; i < creditors.size(); i++) {
        if (shares[i] <= 0) {
          continue;
        }
        String creditorId = creditors.get(i).memberId;
        if (debtor.memberId.equals(creditorId)) {
          continue;
        }
        pairDebt.merge(debtor.memberId + "->" + creditorId, shares[i], Long::sum);
      }
    }
  }

  private static final class Balance {
    private final String memberId;
    private final long amount;

    private Balance(String memberId, long amount) {
      this.memberId = memberId;
      this.amount = amount;
    }
  }

  private static final class Debt {
    private final String from;
    private final String to;
    private final long amount;

    private Debt(String from, String to, long amount) {
      this.from = from;
      this.to = to;
      this.amount = amount;
    }
  }
}
