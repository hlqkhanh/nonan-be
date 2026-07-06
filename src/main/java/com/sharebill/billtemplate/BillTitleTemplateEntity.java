package com.sharebill.billtemplate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "bill_title_templates")
public class BillTitleTemplateEntity {
  @Id
  private String id;

  @Column(name = "owner_user_id", nullable = false)
  private String ownerUserId;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false)
  private int position;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected BillTitleTemplateEntity() {
  }

  public BillTitleTemplateEntity(String id, String ownerUserId, String label, int position, Instant createdAt) {
    this.id = id;
    this.ownerUserId = ownerUserId;
    this.label = label;
    this.position = position;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public String getLabel() {
    return label;
  }

  public int getPosition() {
    return position;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
