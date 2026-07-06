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
@Table(name = "members")
public class MemberEntity {
  @Id
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private GroupEntity group;

  @Column(nullable = false)
  private String name;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected MemberEntity() {
  }

  public MemberEntity(String id, GroupEntity group, String name, String userId, Instant createdAt) {
    this.id = id;
    this.group = group;
    this.name = name;
    this.userId = userId;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public GroupEntity getGroup() {
    return group;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUserId() {
    return userId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
