package com.sharebill.favorite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "favorites")
public class FavoriteEntity {
  @Id
  private String id;

  @Column(name = "owner_user_id", nullable = false)
  private String ownerUserId;

  @Column(name = "target_type", nullable = false)
  private String targetType;

  @Column(name = "target_id", nullable = false)
  private String targetId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected FavoriteEntity() {
  }

  public FavoriteEntity(String id, String ownerUserId, String targetType, String targetId, Instant createdAt) {
    this.id = id;
    this.ownerUserId = ownerUserId;
    this.targetType = targetType;
    this.targetId = targetId;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public String getTargetType() {
    return targetType;
  }

  public String getTargetId() {
    return targetId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
