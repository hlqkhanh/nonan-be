package com.sharebill.expense;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
public class ExpenseEntity {
  @Id
  private String id;

  @Column(name = "group_id", nullable = false)
  private String groupId;

  @Column(name = "ledger_cycle_id", nullable = false)
  private String ledgerCycleId;

  @Column(nullable = false)
  private String title;

  @Column(name = "total_amount", nullable = false)
  private long totalAmount;

  @Column(name = "paid_date", nullable = false)
  private LocalDate paidDate;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "split_mode", nullable = false)
  private String splitMode;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<ExpensePayerEntity> payers = new ArrayList<>();

  @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<ExpenseParticipantEntity> participants = new ArrayList<>();

  protected ExpenseEntity() {
  }

  public ExpenseEntity(String id, String groupId, String ledgerCycleId, String title, long totalAmount,
      LocalDate paidDate, String imageUrl, String splitMode, Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.groupId = groupId;
    this.ledgerCycleId = ledgerCycleId;
    this.title = title;
    this.totalAmount = totalAmount;
    this.paidDate = paidDate;
    this.imageUrl = imageUrl;
    this.splitMode = splitMode;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getLedgerCycleId() {
    return ledgerCycleId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public long getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(long totalAmount) {
    this.totalAmount = totalAmount;
  }

  public LocalDate getPaidDate() {
    return paidDate;
  }

  public void setPaidDate(LocalDate paidDate) {
    this.paidDate = paidDate;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getSplitMode() {
    return splitMode;
  }

  public void setSplitMode(String splitMode) {
    this.splitMode = splitMode;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<ExpensePayerEntity> getPayers() {
    return payers;
  }

  public List<ExpenseParticipantEntity> getParticipants() {
    return participants;
  }
}
