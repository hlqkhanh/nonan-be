package com.sharebill.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "contacts")
public class ContactEntity {
  @Id
  private String id;

  @Column(name = "owner_user_id", nullable = false)
  private String ownerUserId;

  @Column(nullable = false)
  private String name;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ContactEntity() {
  }

  public ContactEntity(String id, String ownerUserId, String name, String avatarUrl, Instant createdAt) {
    this.id = id;
    this.ownerUserId = ownerUserId;
    this.name = name;
    this.avatarUrl = avatarUrl;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
