package com.sharebill.group;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class GroupSharingTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  private TestRestTemplate rest;

  @SuppressWarnings("unchecked")
  @Test
  void groupIsSharedWithUserMembersButOnlyOwnerCanDelete() {
    Map<String, Object> signupA = post("/api/auth/signup", Map.of(
        "email", "groupowner" + System.nanoTime() + "@test.local",
        "password", "password123",
        "displayName", "Alice"
    ), null);
    String tokenA = (String) signupA.get("token");

    Map<String, Object> signupB = post("/api/auth/signup", Map.of(
        "email", "groupmember" + System.nanoTime() + "@test.local",
        "password", "password123",
        "displayName", "Bob"
    ), null);
    String tokenB = (String) signupB.get("token");
    Map<String, Object> userB = (Map<String, Object>) signupB.get("user");
    String userBId = (String) userB.get("id");
    String userBUsername = (String) userB.get("username");

    // A and B become accepted friends.
    post("/api/friends/requests", Map.of("username", userBUsername), tokenA);
    List<Map<String, Object>> requestsForB = (List<Map<String, Object>>) get("/api/friends/requests", tokenB)
        .get("incoming");
    assertThat(requestsForB).isNotEmpty();
    String requestId = (String) requestsForB.get(0).get("id");
    ResponseEntity<Void> acceptResponse = rest.exchange(
        "/api/friends/requests/" + requestId + "/accept", HttpMethod.POST,
        new HttpEntity<>(null, authHeaders(tokenB)), Void.class);
    assertThat(acceptResponse.getStatusCode().is2xxSuccessful()).isTrue();

    // A creates a group including B as a user member.
    Map<String, Object> group = post("/api/groups", Map.of(
        "name", "Trip",
        "members", List.of(Map.of("targetType", "user", "targetId", userBId))
    ), tokenA);
    String groupId = (String) group.get("id");

    // B's GET /api/groups includes that group (shared visibility).
    List<Map<String, Object>> groupsForB = getList("/api/groups", tokenB);
    assertThat(groupsForB).anyMatch(g -> groupId.equals(g.get("id")));

    // B adds one of B's own contacts as a member.
    Map<String, Object> bobContact = post("/api/contacts", Map.of("name", "Charlie"), tokenB);
    String bobContactId = (String) bobContact.get("id");
    ResponseEntity<Map> addMemberResponse = rest.exchange(
        "/api/groups/" + groupId + "/members", HttpMethod.POST,
        new HttpEntity<>(Map.of("targetType", "contact", "targetId", bobContactId), authHeaders(tokenB)), Map.class);
    assertThat(addMemberResponse.getStatusCode().is2xxSuccessful()).isTrue();

    // A's GET reflects the newly added member.
    List<Map<String, Object>> groupsForA = getList("/api/groups", tokenA);
    Map<String, Object> groupSeenByA = groupsForA.stream()
        .filter(g -> groupId.equals(g.get("id")))
        .findFirst()
        .orElseThrow();
    List<Map<String, Object>> membersSeenByA = (List<Map<String, Object>>) groupSeenByA.get("members");
    assertThat(membersSeenByA).anyMatch(m -> "Charlie".equals(m.get("name")));

    // B (non-owner) cannot delete the group.
    ResponseEntity<String> deleteByB = rest.exchange(
        "/api/groups/" + groupId, HttpMethod.DELETE,
        new HttpEntity<>(authHeaders(tokenB)), String.class);
    assertThat(deleteByB.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // A (owner) can delete the group.
    ResponseEntity<String> deleteByA = rest.exchange(
        "/api/groups/" + groupId, HttpMethod.DELETE,
        new HttpEntity<>(authHeaders(tokenA)), String.class);
    assertThat(deleteByA.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  private HttpHeaders authHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> post(String path, Object body, String token) {
    ResponseEntity<Map> response = rest.exchange(path, HttpMethod.POST,
        new HttpEntity<>(body, authHeaders(token)), Map.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    return response.getBody();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> get(String path, String token) {
    ResponseEntity<Map> response = rest.exchange(path, HttpMethod.GET,
        new HttpEntity<>(authHeaders(token)), Map.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    return response.getBody();
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getList(String path, String token) {
    ResponseEntity<List> response = rest.exchange(path, HttpMethod.GET,
        new HttpEntity<>(authHeaders(token)), List.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    return response.getBody();
  }
}
