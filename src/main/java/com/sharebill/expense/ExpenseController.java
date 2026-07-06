package com.sharebill.expense;

import com.sharebill.group.GroupAccessService;
import com.sharebill.group.MemberEntity;
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
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {
  private final ExpenseService expenseService;
  private final GroupAccessService groupAccessService;

  public ExpenseController(ExpenseService expenseService, GroupAccessService groupAccessService) {
    this.expenseService = expenseService;
    this.groupAccessService = groupAccessService;
  }

  @GetMapping
  public List<ExpenseDto> expenses(@PathVariable String groupId, @AuthenticationPrincipal UserEntity user) {
    groupAccessService.requireMember(groupId, user.getId());
    return expenseService.listExpenses(groupId);
  }

  @PostMapping
  public ExpenseDto createExpense(@PathVariable String groupId, @Valid @RequestBody ExpenseDto expense,
      @AuthenticationPrincipal UserEntity user) {
    MemberEntity actor = groupAccessService.requireMember(groupId, user.getId());
    return expenseService.createExpense(groupId, expense, actor.getId());
  }

  @PutMapping("/{expenseId}")
  public ExpenseDto updateExpense(@PathVariable String groupId, @PathVariable String expenseId,
      @Valid @RequestBody ExpenseDto expense, @AuthenticationPrincipal UserEntity user) {
    MemberEntity actor = groupAccessService.requireMember(groupId, user.getId());
    return expenseService.updateExpense(groupId, expenseId, expense, actor.getId());
  }

  @DeleteMapping("/{expenseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteExpense(@PathVariable String groupId, @PathVariable String expenseId,
      @AuthenticationPrincipal UserEntity user) {
    MemberEntity actor = groupAccessService.requireMember(groupId, user.getId());
    expenseService.deleteExpense(groupId, expenseId, actor.getId());
  }
}
