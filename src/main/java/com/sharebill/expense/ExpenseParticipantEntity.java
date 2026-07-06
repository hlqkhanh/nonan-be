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
@Table(name = "expense_participants")
public class ExpenseParticipantEntity {
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

  @Column(name = "is_custom", nullable = false)
  private boolean isCustom;

  @Column(name = "member_name")
  private String memberName;

  @Column(nullable = false)
  private int position;

  protected ExpenseParticipantEntity() {
  }

  public ExpenseParticipantEntity(ExpenseEntity expense, String memberId, long amount, boolean isCustom,
      String memberName, int position) {
    this.expense = expense;
    this.memberId = memberId;
    this.amount = amount;
    this.isCustom = isCustom;
    this.memberName = memberName;
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

  public boolean isCustom() {
    return isCustom;
  }

  public String getMemberName() {
    return memberName;
  }

  public int getPosition() {
    return position;
  }
}
