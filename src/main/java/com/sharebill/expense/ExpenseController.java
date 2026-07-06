package com.sharebill.expense;

import com.sharebill.common.ShareBillService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {
  private final ShareBillService service;

  public ExpenseController(ShareBillService service) {
    this.service = service;
  }

  @GetMapping
  public List<ExpenseDto> expenses(@PathVariable String groupId) {
    return service.expenses(groupId);
  }

  @PostMapping
  public ExpenseDto createExpense(@PathVariable String groupId, @Valid @RequestBody ExpenseDto expense) {
    return service.createExpense(groupId, expense);
  }
}
