package com.sharebill.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_payers")
public class ExpensePayerEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_id", nullable = false)
  private ExpenseEntity expense;

  @Column(name = "member_id", nullable = false)
  private String memberId;

  @Column(nullable = false)
  private long amount;

  @Column(nullable = false)
  private int position;

  protected ExpensePayerEntity() {
  }

  public ExpensePayerEntity(ExpenseEntity expense, String memberId, long amount, int position) {
    this.expense = expense;
    this.memberId = memberId;
    this.amount = amount;
    this.position = position;
  }

  public Long getId() {
    return id;
  }

  public ExpenseEntity getExpense() {
    return expense;
  }

  public String getMemberId() {
    return memberId;
  }

  public long getAmount() {
    return amount;
  }

  public int getPosition() {
    return position;
  }
}
