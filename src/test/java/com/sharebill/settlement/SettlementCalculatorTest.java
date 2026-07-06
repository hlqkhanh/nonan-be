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
        new SettlementDto("b->a:30000", "b", "a", 30000, false),
        new SettlementDto("c->a:30000", "c", "a", 30000, false)
    );
  }

  @Test
  void supportsMultiplePayers() {
    ExpenseDto expense = new ExpenseDto(
        "expense-1",
        "group-1",
        "Dinner",
        90000,
        LocalDate.of(2026, 7, 3),
        null,
        List.of(new PayerContributionDto("a", 50000), new PayerContributionDto("b", 40000)),
        defaultParticipants(),
        "equal"
    );

    assertThat(SettlementCalculator.calculate(List.of(expense), Set.of()))
        .containsExactly(
            new SettlementDto("c->a:20000", "c", "a", 20000, false),
            new SettlementDto("c->b:10000", "c", "b", 10000, false)
        );
  }

  @Test
  void allowsPayerOutsideParticipants() {
    ExpenseDto expense = new ExpenseDto(
        "expense-1",
        "group-1",
        "Dinner",
        90000,
        LocalDate.of(2026, 7, 3),
        null,
        List.of(new PayerContributionDto("x", 90000)),
        defaultParticipants(),
        "equal"
    );

    assertThat(SettlementCalculator.calculate(List.of(expense), Set.of()))
        .allMatch(settlement -> settlement.toMemberId().equals("x"));
  }

  @Test
  void skipsZeroSettlements() {
    ExpenseDto expense = new ExpenseDto(
        "expense-1",
        "group-1",
        "Free",
        0,
        LocalDate.of(2026, 7, 3),
        null,
        List.of(new PayerContributionDto("a", 0)),
        List.of(new ParticipantShareDto("a", 0, false)),
        "equal"
    );

    assertThat(SettlementCalculator.calculate(List.of(expense), Set.of())).isEmpty();
  }

  @Test
  void marksPaidSettlements() {
    assertThat(SettlementCalculator.calculate(List.of(defaultExpense()), Set.of("b->a:30000")).getFirst().paid()).isTrue();
  }

  private static ExpenseDto defaultExpense() {
    return new ExpenseDto(
        "expense-1",
        "group-1",
        "Dinner",
        90000,
        LocalDate.of(2026, 7, 3),
        null,
        List.of(new PayerContributionDto("a", 90000)),
        defaultParticipants(),
        "equal"
    );
  }

  private static List<ParticipantShareDto> defaultParticipants() {
    return List.of(
        new ParticipantShareDto("a", 30000, false),
        new ParticipantShareDto("b", 30000, false),
        new ParticipantShareDto("c", 30000, false)
    );
  }
}
