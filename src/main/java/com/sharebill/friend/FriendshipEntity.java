package com.sharebill.friend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "friendships")
public class FriendshipEntity {
  public static final String STATUS_PENDING = "pending";
  public static final String STATUS_ACCEPTED = "accepted";

  @Id
  private String id;

  @Column(name = "requester_user_id", nullable = false)
  private String requesterUserId;

  @Column(name = "addressee_user_id", nullable = false)
  private String addresseeUserId;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "responded_at")
  private Instant respondedAt;

  protected FriendshipEntity() {
  }

  public FriendshipEntity(String id, String requesterUserId, String addresseeUserId, String status, Instant createdAt) {
    this.id = id;
    this.requesterUserId = requesterUserId;
    this.addresseeUserId = addresseeUserId;
    this.status = status;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getRequesterUserId() {
    return requesterUserId;
  }

  public String getAddresseeUserId() {
    return addresseeUserId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getRespondedAt() {
    return respondedAt;
  }

  public void setRespondedAt(Instant respondedAt) {
    this.respondedAt = respondedAt;
  }

  public String otherUserId(String userId) {
    return requesterUserId.equals(userId) ? addresseeUserId : requesterUserId;
  }
}
