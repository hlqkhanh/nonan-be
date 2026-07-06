package com.sharebill.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.sharebill.common.SettlementCalculator;
import com.sharebill.expense.ExpenseDto;
import com.sharebill.expense.ParticipantShareDto;
import com.sharebill.expense.PayerContributionDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SettlementCalculatorTest {
  @Test
  void createsSettlementsWhenOnePayerCoversBill() {
    List<SettlementDto> settlements = SettlementCalculator.calculate(List.of(defaultExpense()), Set.of());

    assertThat(settlements).containsExactly(
        new SettlementDto("b->a", "b", "a", 30000, false),
        new SettlementDto("c->a", "c", "a", 30000, false)
    );
  }

  @Test
  void supportsMultiplePayers() {
    ExpenseDto expense = new ExpenseDto(
        "expense-1",
        "Dinner",
        90000,
        LocalDate.of(2026, 7, 3),
        null,
        List.of(new PayerContributionDto("a", 50000), new PayerContributionDto("b", 40000)),
        defaultParticipants(),
        "equal",
        "cycle-1"
    );

    assertThat(SettlementCalculator.calculate(List.of(expense), Set.of()))
        .containsExactly(
            new SettlementDto("c->a", "c", "a", 20000, false),
            new SettlementDto("c->b", "c", "b", 10000, false)
        );
  }

  @Test
  void allowsPayerOutsideParticipants() {
    ExpenseDto expense = new ExpenseDto(
        "expense-1",
        "Dinner",
        90000,
        LocalDate.of(2026, 7, 3),
        null,
        List.of(new PayerContributionDto("x", 90000)),
        defaultParticipants(),
        "equal",
        "cycle-1"
    );

    assertThat(SettlementCalculator.calculate(List.of(expense), Set.of()))
        .allMatch(settlement -> settlement.toMemberId().equals("x"));
  }

  @Test
  void skipsZeroSettlements() {
    ExpenseDto expense = new ExpenseDto(
        "expense-1",
        "Free",
        0,
        LocalDate.of(2026, 7, 3),
        null,
        List.of(new PayerContributionDto("a", 0)),
        List.of(new ParticipantShareDto("a", 0, false, null)),
        "equal",
        "cycle-1"
    );

    assertThat(SettlementCalculator.calculate(List.of(expense), Set.of())).isEmpty();
  }

  @Test
  void marksPaidSettlements() {
    assertThat(SettlementCalculator.calculate(List.of(defaultExpense()), Set.of("b->a")).get(0).paid()).isTrue();
  }

  private static ExpenseDto defaultExpense() {
    return new ExpenseDto(
        "expense-1",
        "Dinner",
        90000,
        LocalDate.of(2026, 7, 3),
        null,
        List.of(new PayerContributionDto("a", 90000)),
        defaultParticipants(),
        "equal",
        "cycle-1"
    );
  }

  private static List<ParticipantShareDto> defaultParticipants() {
    return List.of(
        new ParticipantShareDto("a", 30000, false, null),
        new ParticipantShareDto("b", 30000, false, null),
        new ParticipantShareDto("c", 30000, false, null)
    );
  }
}
