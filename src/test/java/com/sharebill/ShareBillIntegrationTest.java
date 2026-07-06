package com.sharebill;

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
class ShareBillIntegrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  private TestRestTemplate rest;

  @SuppressWarnings("unchecked")
  @Test
  void fullLedgerLifecycle() {
    Map<String, Object> signupA = post("/api/auth/signup", Map.of(
        "email", "alice" + System.nanoTime() + "@test.local",
        "password", "password123",
        "displayName", "Alice"
    ), null);
    String tokenA = (String) signupA.get("token");

    Map<String, Object> group = post("/api/groups", Map.of("name", "Trip"), tokenA);
    String groupId = (String) group.get("id");
    List<Map<String, Object>> members = (List<Map<String, Object>>) group.get("members");
    String creatorMemberId = (String) members.get(0).get("id");
    assertThat(members.get(0).get("userId")).isNotNull();

    Map<String, Object> groupAfterMember = post("/api/groups/" + groupId + "/members", Map.of("name", "Bob"), tokenA);
    List<Map<String, Object>> membersAfter = (List<Map<String, Object>>) groupAfterMember.get("members");
    String ghostId = membersAfter.stream()
        .filter(m -> "Bob".equals(m.get("name")))
        .findFirst().orElseThrow().get("id").toString();

    Map<String, Object> expense = Map.of(
        "id", "exp-1",
        "groupId", groupId,
        "title", "Dinner",
        "totalAmount", 100000,
        "paidDate", "2026-07-06",
        "payers", List.of(Map.of("memberId", creatorMemberId, "amount", 100000)),
        "participants", List.of(
            Map.of("memberId", creatorMemberId, "amount", 50000, "isCustom", false),
            Map.of("memberId", ghostId, "amount", 50000, "isCustom", false)
        ),
        "splitMode", "equal"
    );
    Map<String, Object> createdExpense = post("/api/groups/" + groupId + "/expenses", expense, tokenA);
    assertThat(createdExpense.get("ledgerCycleId")).isNotNull();

    Map<String, Object> current = get("/api/groups/" + groupId + "/ledger/current", tokenA);
    Map<String, Object> cycle = (Map<String, Object>) current.get("cycle");
    String cycleId = (String) cycle.get("id");
    List<Map<String, Object>> settlements = (List<Map<String, Object>>) current.get("settlements");
    assertThat(settlements).hasSize(1);
    String settlementId = (String) settlements.get(0).get("id");
    assertThat(settlementId).isEqualTo(ghostId + "->" + creatorMemberId);

    List<Map<String, Object>> afterAdjust = postList(
        "/api/groups/" + groupId + "/ledger/cycles/" + cycleId + "/settlements/adjust",
        Map.of("settlementId", settlementId, "deltaAmount", 5000), tokenA);
    assertThat(((Number) afterAdjust.get(0).get("amount")).longValue()).isEqualTo(55000L);

    List<Map<String, Object>> afterMarkPaid = postList(
        "/api/groups/" + groupId + "/ledger/cycles/" + cycleId + "/settlements/mark-paid",
        Map.of("settlementId", settlementId), tokenA);
    assertThat((Boolean) afterMarkPaid.get(0).get("paid")).isTrue();

    Map<String, Object> settled = post("/api/groups/" + groupId + "/ledger/current/settle", Map.of(), tokenA);
    Map<String, Object> settledCycle = (Map<String, Object>) settled.get("cycle");
    assertThat(settledCycle.get("status")).isEqualTo("settled");
    List<Map<String, Object>> snapshotSettlements = (List<Map<String, Object>>) settled.get("settlements");
    assertThat((Boolean) snapshotSettlements.get(0).get("paid")).isTrue();
    List<Map<String, Object>> auditLogs = (List<Map<String, Object>>) settled.get("auditLogs");
    assertThat(auditLogs).isNotEmpty();

    Map<String, Object> newCurrent = get("/api/groups/" + groupId + "/ledger/current", tokenA);
    Map<String, Object> newCycle = (Map<String, Object>) newCurrent.get("cycle");
    assertThat(newCycle.get("id")).isNotEqualTo(cycleId);
    assertThat((List<?>) newCurrent.get("expenses")).isEmpty();

    ResponseEntity<String> putResponse = rest.exchange(
        "/api/groups/" + groupId + "/expenses/exp-1", HttpMethod.PUT,
        new HttpEntity<>(expense, authHeaders(tokenA)), String.class);
    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<String> noAuth = rest.getForEntity("/api/auth/me", String.class);
    assertThat(noAuth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    Map<String, Object> signupB = post("/api/auth/signup", Map.of(
        "email", "carol" + System.nanoTime() + "@test.local",
        "password", "password123",
        "displayName", "Carol"
    ), null);
    String tokenB = (String) signupB.get("token");

    ResponseEntity<String> forbidden = rest.exchange(
        "/api/groups/" + groupId + "/expenses", HttpMethod.GET,
        new HttpEntity<>(authHeaders(tokenB)), String.class);
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
  private List<Map<String, Object>> postList(String path, Object body, String token) {
    ResponseEntity<List> response = rest.exchange(path, HttpMethod.POST,
        new HttpEntity<>(body, authHeaders(token)), List.class);
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
}
