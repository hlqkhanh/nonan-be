package com.sharebill.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.sharebill.common.SettlementCalculator;
import com.sharebill.expense.ExpenseDto;
import com.sharebill.expense.ParticipantShareDto;
import com.sharebill.expense.PayerContributionDto;
import java.time.LocalDateTime;
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
        LocalDateTime.of(2026, 7, 3, 12, 0),
        null,
        List.of(new PayerContributionDto("a", 50000), new PayerContributionDto("b", 40000)),
        defaultParticipants(),
        "equal",
        "cycle-1",
        null
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
        LocalDateTime.of(2026, 7, 3, 12, 0),
        null,
        List.of(new PayerContributionDto("x", 90000)),
        defaultParticipants(),
        "equal",
        "cycle-1",
        null
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
        LocalDateTime.of(2026, 7, 3, 12, 0),
        null,
        List.of(new PayerContributionDto("a", 0)),
        List.of(new ParticipantShareDto("a", 0, false, null)),
        "equal",
        "cycle-1",
        null
    );

    assertThat(SettlementCalculator.calculate(List.of(expense), Set.of())).isEmpty();
  }

  @Test
  void marksPaidSettlements() {
    assertThat(SettlementCalculator.calculate(List.of(defaultExpense()), Set.of("b->a")).get(0).paid()).isTrue();
  }

  @Test
  void doesNotOffsetDebtsThroughThirdParty() {
    ExpenseDto expense1 = new ExpenseDto(
        "expense-1",
        "Coffee",
        20000,
        LocalDateTime.of(2026, 7, 3, 12, 0),
        null,
        List.of(new PayerContributionDto("b", 20000)),
        List.of(
            new ParticipantShareDto("a", 10000, false, null),
            new ParticipantShareDto("b", 10000, false, null)
        ),
        "equal",
        "cycle-1",
        null
    );
    ExpenseDto expense2 = new ExpenseDto(
        "expense-2",
        "Snack",
        20000,
        LocalDateTime.of(2026, 7, 4, 12, 0),
        null,
        List.of(new PayerContributionDto("c", 20000)),
        List.of(
            new ParticipantShareDto("b", 10000, false, null),
            new ParticipantShareDto("c", 10000, false, null)
        ),
        "equal",
        "cycle-1",
        null
    );

    assertThat(SettlementCalculator.calculate(List.of(expense1, expense2), Set.of()))
        .containsExactly(
            new SettlementDto("a->b", "a", "b", 10000, false),
            new SettlementDto("b->c", "b", "c", 10000, false)
        );
  }

  @Test
  void netsBidirectionalDebtsWithinPair() {
    ExpenseDto expense1 = new ExpenseDto(
        "expense-1",
        "Dinner",
        100000,
        LocalDateTime.of(2026, 7, 3, 12, 0),
        null,
        List.of(new PayerContributionDto("a", 100000)),
        List.of(
            new ParticipantShareDto("a", 50000, false, null),
            new ParticipantShareDto("b", 50000, false, null)
        ),
        "equal",
        "cycle-1",
        null
    );
    ExpenseDto expense2 = new ExpenseDto(
        "expense-2",
        "Lunch",
        60000,
        LocalDateTime.of(2026, 7, 4, 12, 0),
        null,
        List.of(new PayerContributionDto("b", 60000)),
        List.of(
            new ParticipantShareDto("a", 30000, false, null),
            new ParticipantShareDto("b", 30000, false, null)
        ),
        "equal",
        "cycle-1",
        null
    );

    assertThat(SettlementCalculator.calculate(List.of(expense1, expense2), Set.of()))
        .containsExactly(
            new SettlementDto("b->a", "b", "a", 20000, false)
        );
  }

  @Test
  void cancelsFullyOffsettingPair() {
    ExpenseDto expense1 = new ExpenseDto(
        "expense-1",
        "Dinner",
        100000,
        LocalDateTime.of(2026, 7, 3, 12, 0),
        null,
        List.of(new PayerContributionDto("a", 100000)),
        List.of(
            new ParticipantShareDto("a", 50000, false, null),
            new ParticipantShareDto("b", 50000, false, null)
        ),
        "equal",
        "cycle-1",
        null
    );
    ExpenseDto expense2 = new ExpenseDto(
        "expense-2",
        "Lunch",
        100000,
        LocalDateTime.of(2026, 7, 4, 12, 0),
        null,
        List.of(new PayerContributionDto("b", 100000)),
        List.of(
            new ParticipantShareDto("a", 50000, false, null),
            new ParticipantShareDto("b", 50000, false, null)
        ),
        "equal",
        "cycle-1",
        null
    );

    assertThat(SettlementCalculator.calculate(List.of(expense1, expense2), Set.of())).isEmpty();
  }

  @Test
  void allocatesProportionallyWithMultipleDebtorsAndCreditors() {
    ExpenseDto expense = new ExpenseDto(
        "expense-1",
        "Party",
        20000,
        LocalDateTime.of(2026, 7, 3, 12, 0),
        null,
        List.of(new PayerContributionDto("a", 12000), new PayerContributionDto("b", 8000)),
        List.of(
            new ParticipantShareDto("a", 5000, false, null),
            new ParticipantShareDto("b", 5000, false, null),
            new ParticipantShareDto("c", 5000, false, null),
            new ParticipantShareDto("d", 5000, false, null)
        ),
        "equal",
        "cycle-1",
        null
    );

    assertThat(SettlementCalculator.calculate(List.of(expense), Set.of()))
        .containsExactly(
            new SettlementDto("c->a", "c", "a", 3500, false),
            new SettlementDto("c->b", "c", "b", 1500, false),
            new SettlementDto("d->a", "d", "a", 3500, false),
            new SettlementDto("d->b", "d", "b", 1500, false)
        );
  }

  @Test
  void assignsRoundingRemainderToLargestCreditor() {
    ExpenseDto expense = new ExpenseDto(
        "expense-1",
        "Party",
        10000,
        LocalDateTime.of(2026, 7, 3, 12, 0),
        null,
        List.of(new PayerContributionDto("a", 5000), new PayerContributionDto("b", 5000)),
        List.of(
            new ParticipantShareDto("c", 7001, false, null),
            new ParticipantShareDto("d", 2999, false, null)
        ),
        "equal",
        "cycle-1",
        null
    );

    List<SettlementDto> settlements = SettlementCalculator.calculate(List.of(expense), Set.of());

    assertThat(settlements)
        .containsExactly(
            new SettlementDto("c->a", "c", "a", 3501, false),
            new SettlementDto("c->b", "c", "b", 3500, false),
            new SettlementDto("d->a", "d", "a", 1500, false),
            new SettlementDto("d->b", "d", "b", 1499, false)
        );
    assertThat(settlements.stream().mapToLong(SettlementDto::amount).sum()).isEqualTo(10000);
  }

  private static ExpenseDto defaultExpense() {
    return new ExpenseDto(
        "expense-1",
        "Dinner",
        90000,
        LocalDateTime.of(2026, 7, 3, 12, 0),
        null,
        List.of(new PayerContributionDto("a", 90000)),
        defaultParticipants(),
        "equal",
        "cycle-1",
        null
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
