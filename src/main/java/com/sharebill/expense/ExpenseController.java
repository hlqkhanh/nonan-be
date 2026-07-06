package com.sharebill.expense;

import com.sharebill.user.UserEntity;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
  private final ExpenseService expenseService;

  public ExpenseController(ExpenseService expenseService) {
    this.expenseService = expenseService;
  }

  @GetMapping
  public List<ExpenseDto> expenses(@AuthenticationPrincipal UserEntity user) {
    return expenseService.listExpenses(user.getId());
  }

  @PostMapping
  public ExpenseDto createExpense(@Valid @RequestBody ExpenseDto expense, @AuthenticationPrincipal UserEntity user) {
    return expenseService.createExpense(user.getId(), expense);
  }

  @PutMapping("/{expenseId}")
  public ExpenseDto updateExpense(@PathVariable String expenseId, @Valid @RequestBody ExpenseDto expense,
      @AuthenticationPrincipal UserEntity user) {
    return expenseService.updateExpense(user.getId(), expenseId, expense);
  }

  @DeleteMapping("/{expenseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteExpense(@PathVariable String expenseId, @AuthenticationPrincipal UserEntity user) {
    expenseService.deleteExpense(user.getId(), expenseId);
  }
}
