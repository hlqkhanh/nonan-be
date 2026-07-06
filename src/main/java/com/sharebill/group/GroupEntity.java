package com.sharebill.group;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
public class GroupEntity {
  @Id
  private String id;

  @Column(nullable = false)
  private String name;

  @Column(name = "created_by_user_id")
  private String createdByUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private List<GroupMemberEntity> members = new ArrayList<>();

  protected GroupEntity() {
  }

  public GroupEntity(String id, String name, String createdByUserId, Instant createdAt) {
    this.id = id;
    this.name = name;
    this.createdByUserId = createdByUserId;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCreatedByUserId() {
    return createdByUserId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public List<GroupMemberEntity> getMembers() {
    return members;
  }
}
