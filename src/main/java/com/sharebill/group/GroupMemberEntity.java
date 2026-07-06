package com.sharebill.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "group_members")
public class GroupMemberEntity {
  @Id
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private GroupEntity group;

  @Column(name = "target_type", nullable = false)
  private String targetType;

  @Column(name = "target_id", nullable = false)
  private String targetId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected GroupMemberEntity() {
  }

  public GroupMemberEntity(String id, GroupEntity group, String targetType, String targetId, Instant createdAt) {
    this.id = id;
    this.group = group;
    this.targetType = targetType;
    this.targetId = targetId;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public GroupEntity getGroup() {
    return group;
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
