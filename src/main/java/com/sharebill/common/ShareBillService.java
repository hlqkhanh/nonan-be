package com.sharebill.common;

import com.sharebill.expense.ExpenseDto;
import com.sharebill.expense.ParticipantShareDto;
import com.sharebill.expense.PayerContributionDto;
import com.sharebill.group.GroupDto;
import com.sharebill.group.MemberDto;
import com.sharebill.settlement.SettlementDto;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ShareBillService {
  private final Map<String, GroupDto> groups = new LinkedHashMap<>();
  private final Map<String, List<ExpenseDto>> expensesByGroup = new HashMap<>();
  private final Set<String> paidSettlementIds = new HashSet<>();

  @PostConstruct
  void seed() {
    GroupDto group = new GroupDto(
        "group-1",
        "Hoi Ban Tron",
        List.of(
            new MemberDto("khanh", "Khanh"),
            new MemberDto("kien", "Kien"),
            new MemberDto("thong", "Thong"),
            new MemberDto("nam", "Nam")
        )
    );

    groups.put(group.id(), group);
    expensesByGroup.put(group.id(), new ArrayList<>(List.of(
        new ExpenseDto(
            "expense-1",
            group.id(),
            "Lau toi thu sau",
            320000,
            LocalDate.of(2026, 7, 3),
            "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=600&q=80",
            List.of(new PayerContributionDto("khanh", 320000)),
            List.of(
                new ParticipantShareDto("khanh", 80000, false),
                new ParticipantShareDto("kien", 80000, false),
                new ParticipantShareDto("thong", 80000, false),
                new ParticipantShareDto("nam", 80000, false)
            ),
            "equal"
        ),
        new ExpenseDto(
            "expense-2",
            group.id(),
            "Cafe sau phim",
            185000,
            LocalDate.of(2026, 7, 4),
            null,
            List.of(new PayerContributionDto("kien", 100000), new PayerContributionDto("thong", 85000)),
            List.of(
                new ParticipantShareDto("khanh", 61667, false),
                new ParticipantShareDto("kien", 61667, false),
                new ParticipantShareDto("thong", 61666, false)
            ),
            "equal"
        )
    )));
  }

  public List<GroupDto> groups() {
    return List.copyOf(groups.values());
  }

  public GroupDto createGroup(GroupDto group) {
    GroupDto normalized = new GroupDto(group.id(), group.name(), group.members() == null ? List.of() : group.members());
    groups.put(normalized.id(), normalized);
    expensesByGroup.putIfAbsent(normalized.id(), new ArrayList<>());
    return normalized;
  }

  public GroupDto addMember(String groupId, MemberDto member) {
    GroupDto group = requireGroup(groupId);
    List<MemberDto> members = new ArrayList<>(group.members());
    members.removeIf(existing -> existing.id().equals(member.id()));
    members.add(member);
    GroupDto updated = new GroupDto(group.id(), group.name(), List.copyOf(members));
    groups.put(groupId, updated);
    return updated;
  }

  public List<ExpenseDto> expenses(String groupId) {
    requireGroup(groupId);
    return expensesByGroup.getOrDefault(groupId, List.of()).stream()
        .sorted(Comparator.comparing(ExpenseDto::paidDate).reversed())
        .toList();
  }

  public ExpenseDto createExpense(String groupId, ExpenseDto expense) {
    requireGroup(groupId);
    validateExpense(groupId, expense);
    List<ExpenseDto> expenses = expensesByGroup.computeIfAbsent(groupId, ignored -> new ArrayList<>());
    expenses.removeIf(existing -> existing.id().equals(expense.id()));
    expenses.add(0, expense);
    return expense;
  }

  public List<SettlementDto> settlements(String groupId) {
    requireGroup(groupId);
    return SettlementCalculator.calculate(expensesByGroup.getOrDefault(groupId, List.of()), paidSettlementIds);
  }

  public List<SettlementDto> markSettlementPaid(String groupId, String settlementId) {
    requireGroup(groupId);
    if (paidSettlementIds.contains(settlementId)) {
      paidSettlementIds.remove(settlementId);
    } else {
      paidSettlementIds.add(settlementId);
    }
    return settlements(groupId);
  }

  private GroupDto requireGroup(String groupId) {
    GroupDto group = groups.get(groupId);
    if (group == null) {
      throw new IllegalArgumentException("Group not found: " + groupId);
    }
    return group;
  }

  private void validateExpense(String groupId, ExpenseDto expense) {
    if (!groupId.equals(expense.groupId())) {
      throw new IllegalArgumentException("Expense groupId must match path groupId");
    }

    long payerTotal = expense.payers().stream().mapToLong(PayerContributionDto::amount).sum();
    long shareTotal = expense.participants().stream().mapToLong(ParticipantShareDto::amount).sum();

    if (payerTotal != expense.totalAmount()) {
      throw new IllegalArgumentException("Payer total must equal expense total");
    }

    if (shareTotal != expense.totalAmount()) {
      throw new IllegalArgumentException("Participant share total must equal expense total");
    }
  }
}
