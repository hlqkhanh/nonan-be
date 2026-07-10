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
  void fullPersonalLedgerLifecycle() {
    Map<String, Object> signupA = post("/api/auth/signup", Map.of(
        "email", "alice" + System.nanoTime() + "@test.local",
        "password", "password123",
        "displayName", "Alice"
    ), null);
    String tokenA = (String) signupA.get("token");
    Map<String, Object> userA = (Map<String, Object>) signupA.get("user");
    String userAId = (String) userA.get("id");
    String selfParticipantId = "user:" + userAId;

    Map<String, Object> contact = post("/api/contacts", Map.of("name", "Bob"), tokenA);
    String contactParticipantId = "contact:" + contact.get("id");

    Map<String, Object> expense = Map.of(
        "id", "exp-1",
        "title", "Dinner",
        "totalAmount", 100000,
        "paidDate", "2026-07-06",
        "payers", List.of(Map.of("memberId", selfParticipantId, "amount", 100000)),
        "participants", List.of(
            Map.of("memberId", selfParticipantId, "amount", 50000, "isCustom", false),
            Map.of("memberId", contactParticipantId, "amount", 50000, "isCustom", false)
        ),
        "splitMode", "equal"
    );
    Map<String, Object> createdExpense = post("/api/expenses", expense, tokenA);
    assertThat(createdExpense.get("ledgerCycleId")).isNotNull();
    List<Map<String, Object>> createdParticipants = (List<Map<String, Object>>) createdExpense.get("participants");
    assertThat(createdParticipants).anyMatch(p -> "Bob".equals(p.get("memberName")));

    Map<String, Object> current = get("/api/ledger/current", tokenA);
    Map<String, Object> cycle = (Map<String, Object>) current.get("cycle");
    String cycleId = (String) cycle.get("id");
    List<Map<String, Object>> settlements = (List<Map<String, Object>>) current.get("settlements");
    assertThat(settlements).hasSize(1);
    String settlementId = (String) settlements.get(0).get("id");
    assertThat(settlementId).isEqualTo(contactParticipantId + "->" + selfParticipantId);

    List<Map<String, Object>> afterAdjust = postList(
        "/api/ledger/cycles/" + cycleId + "/settlements/adjust",
        Map.of("settlementId", settlementId, "deltaAmount", 5000), tokenA);
    assertThat(((Number) afterAdjust.get(0).get("amount")).longValue()).isEqualTo(55000L);

    List<Map<String, Object>> afterMarkPaid = postList(
        "/api/ledger/cycles/" + cycleId + "/settlements/mark-paid",
        Map.of("settlementId", settlementId), tokenA);
    assertThat((Boolean) afterMarkPaid.get(0).get("paid")).isTrue();

    Map<String, Object> settled = post("/api/ledger/current/settle", Map.of(), tokenA);
    Map<String, Object> settledCycle = (Map<String, Object>) settled.get("cycle");
    assertThat(settledCycle.get("status")).isEqualTo("settled");
    List<Map<String, Object>> snapshotSettlements = (List<Map<String, Object>>) settled.get("settlements");
    assertThat((Boolean) snapshotSettlements.get(0).get("paid")).isTrue();
    List<Map<String, Object>> auditLogs = (List<Map<String, Object>>) settled.get("auditLogs");
    assertThat(auditLogs).isNotEmpty();

    Map<String, Object> newCurrent = get("/api/ledger/current", tokenA);
    Map<String, Object> newCycle = (Map<String, Object>) newCurrent.get("cycle");
    assertThat(newCycle.get("id")).isNotEqualTo(cycleId);
    assertThat((List<?>) newCurrent.get("expenses")).isEmpty();

    ResponseEntity<String> putResponse = rest.exchange(
        "/api/expenses/exp-1", HttpMethod.PUT,
        new HttpEntity<>(expense, authHeaders(tokenA)), String.class);
    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<String> noAuth = rest.getForEntity("/api/auth/me", String.class);
    assertThat(noAuth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // A user unrelated to any bill still has their own empty ledger (isolation still holds —
    // shared visibility is only granted via ledger_cycle_members, i.e. by being a bill participant).
    Map<String, Object> signupB = post("/api/auth/signup", Map.of(
        "email", "carol" + System.nanoTime() + "@test.local",
        "password", "password123",
        "displayName", "Carol"
    ), null);
    String tokenB = (String) signupB.get("token");

    Map<String, Object> currentB = get("/api/ledger/current", tokenB);
    assertThat((List<?>) currentB.get("expenses")).isEmpty();
    List<Map<String, Object>> expensesB = getList("/api/expenses", tokenB);
    assertThat(expensesB).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Test
  void sharedLedgerVisibleToParticipantsWithPinAndReopen() {
    Map<String, Object> signupA = post("/api/auth/signup", Map.of(
        "email", "dana" + System.nanoTime() + "@test.local",
        "password", "password123",
        "displayName", "Dana"
    ), null);
    String tokenA = (String) signupA.get("token");
    Map<String, Object> userA = (Map<String, Object>) signupA.get("user");
    String userAId = (String) userA.get("id");

    Map<String, Object> signupB = post("/api/auth/signup", Map.of(
        "email", "erin" + System.nanoTime() + "@test.local",
        "password", "password123",
        "displayName", "Erin"
    ), null);
    String tokenB = (String) signupB.get("token");
    Map<String, Object> userB = (Map<String, Object>) signupB.get("user");
    String userBId = (String) userB.get("id");
    String userBUsername = (String) userB.get("username");

    // A and B become accepted friends.
    post("/api/friends/requests", Map.of("username", userBUsername), tokenA);
    List<Map<String, Object>> requestsForB = (List<Map<String, Object>>) get("/api/friends/requests", tokenB)
        .get("incoming");
    String requestId = (String) requestsForB.get(0).get("id");
    rest.exchange("/api/friends/requests/" + requestId + "/accept", HttpMethod.POST,
        new HttpEntity<>(null, authHeaders(tokenB)), Void.class);

    String memberA = "user:" + userAId;
    String memberB = "user:" + userBId;

    // A creates a bill with B as a participant/payer -> the bill lands in A's own open cycle,
    // but B becomes a member of that shared cycle.
    Map<String, Object> bill = Map.of(
        "id", "shared-exp-1",
        "title", "Nhau",
        "totalAmount", 150000,
        "paidDate", "2026-07-06",
        "payers", List.of(Map.of("memberId", memberA, "amount", 150000)),
        "participants", List.of(
            Map.of("memberId", memberA, "amount", 75000, "isCustom", false),
            Map.of("memberId", memberB, "amount", 75000, "isCustom", false)
        ),
        "splitMode", "equal"
    );
    Map<String, Object> createdBill = post("/api/expenses", bill, tokenA);
    assertThat(createdBill.get("createdByDisplayName")).isEqualTo("Dana");
    String cycleId = (String) createdBill.get("ledgerCycleId");

    // B sees the shared cycle in /cycles, pinned by default, and can fetch its detail.
    List<Map<String, Object>> cyclesForB = getList("/api/ledger/cycles", tokenB);
    Map<String, Object> cycleSeenByB = cyclesForB.stream()
        .filter(c -> cycleId.equals(c.get("id")))
        .findFirst()
        .orElseThrow();
    assertThat((Boolean) cycleSeenByB.get("pinned")).isTrue();
    assertThat((Boolean) cycleSeenByB.get("isOwner")).isFalse();
    assertThat(cycleSeenByB.get("ownerDisplayName")).isEqualTo("Dana");

    Map<String, Object> detailForB = get("/api/ledger/cycles/" + cycleId, tokenB);
    Map<String, Object> membersMap = (Map<String, Object>) detailForB.get("members");
    assertThat(membersMap).containsKey(memberA);
    assertThat(membersMap).containsKey(memberB);

    // A creates a second bill also involving B, so B's membership survives even after the first
    // bill (the only other thing tying B to this cycle) gets edited/deleted below.
    Map<String, Object> bill2 = Map.of(
        "id", "shared-exp-2",
        "title", "Cafe",
        "totalAmount", 60000,
        "paidDate", "2026-07-06",
        "payers", List.of(Map.of("memberId", memberA, "amount", 60000)),
        "participants", List.of(
            Map.of("memberId", memberA, "amount", 30000, "isCustom", false),
            Map.of("memberId", memberB, "amount", 30000, "isCustom", false)
        ),
        "splitMode", "equal"
    );
    post("/api/expenses", bill2, tokenA);

    // B edits the bill -> audit log carries B's name and the amount change.
    Map<String, Object> editedBill = Map.of(
        "id", "shared-exp-1",
        "title", "Nhau",
        "totalAmount", 100000,
        "paidDate", "2026-07-06",
        "payers", List.of(Map.of("memberId", memberA, "amount", 100000)),
        "participants", List.of(
            Map.of("memberId", memberA, "amount", 50000, "isCustom", false),
            Map.of("memberId", memberB, "amount", 50000, "isCustom", false)
        ),
        "splitMode", "equal"
    );
    ResponseEntity<Map> editResponse = rest.exchange(
        "/api/expenses/shared-exp-1", HttpMethod.PUT,
        new HttpEntity<>(editedBill, authHeaders(tokenB)), Map.class);
    assertThat(editResponse.getStatusCode().is2xxSuccessful()).isTrue();

    Map<String, Object> detailAfterEdit = get("/api/ledger/cycles/" + cycleId, tokenA);
    List<Map<String, Object>> auditAfterEdit = (List<Map<String, Object>>) detailAfterEdit.get("auditLogs");
    assertThat(auditAfterEdit).anyMatch(a -> ((String) a.get("summary")).contains("Erin")
        && ((String) a.get("summary")).contains("150000")
        && ((String) a.get("summary")).contains("100000"));

    // B soft-deletes the bill -> disappears from detail, but the audit trail shows B deleted it.
    ResponseEntity<Void> deleteResponse = rest.exchange(
        "/api/expenses/shared-exp-1", HttpMethod.DELETE,
        new HttpEntity<>(authHeaders(tokenB)), Void.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    Map<String, Object> detailAfterDelete = get("/api/ledger/cycles/" + cycleId, tokenA);
    List<Map<String, Object>> expensesAfterDelete = (List<Map<String, Object>>) detailAfterDelete.get("expenses");
    assertThat(expensesAfterDelete).noneMatch(e -> "shared-exp-1".equals(e.get("id")));
    assertThat(expensesAfterDelete).anyMatch(e -> "shared-exp-2".equals(e.get("id")));
    List<Map<String, Object>> auditAfterDelete = (List<Map<String, Object>>) detailAfterDelete.get("auditLogs");
    assertThat(auditAfterDelete).anyMatch(a -> ((String) a.get("summary")).contains("Erin")
        && ((String) a.get("summary")).contains("đã xóa"));

    // B unpins the shared cycle -> only affects B; A still sees it pinned.
    Map<String, Object> unpinnedForB = post("/api/ledger/cycles/" + cycleId + "/unpin", Map.of(), tokenB);
    Map<String, Object> unpinnedCycleForB = (Map<String, Object>) unpinnedForB.get("cycle");
    assertThat((Boolean) unpinnedCycleForB.get("pinned")).isFalse();

    List<Map<String, Object>> cyclesForBAfterUnpin = getList("/api/ledger/cycles", tokenB);
    Map<String, Object> cycleSeenByBAfterUnpin = cyclesForBAfterUnpin.stream()
        .filter(c -> cycleId.equals(c.get("id")))
        .findFirst()
        .orElseThrow();
    assertThat((Boolean) cycleSeenByBAfterUnpin.get("pinned")).isFalse();

    List<Map<String, Object>> cyclesForA = getList("/api/ledger/cycles", tokenA);
    Map<String, Object> cycleSeenByA = cyclesForA.stream()
        .filter(c -> cycleId.equals(c.get("id")))
        .findFirst()
        .orElseThrow();
    assertThat((Boolean) cycleSeenByA.get("pinned")).isTrue();

    // B (any member) settles the cycle -> closed for everyone.
    Map<String, Object> settledByB = post("/api/ledger/cycles/" + cycleId + "/settle", Map.of(), tokenB);
    Map<String, Object> settledCycle = (Map<String, Object>) settledByB.get("cycle");
    assertThat(settledCycle.get("status")).isEqualTo("settled");

    Map<String, Object> detailForAAfterSettle = get("/api/ledger/cycles/" + cycleId, tokenA);
    Map<String, Object> cycleForAAfterSettle = (Map<String, Object>) detailForAAfterSettle.get("cycle");
    assertThat(cycleForAAfterSettle.get("status")).isEqualTo("settled");

    // A's next bill lazily opens a brand-new cycle for A (no eager creation on close).
    Map<String, Object> currentForA = get("/api/ledger/current", tokenA);
    Map<String, Object> newCycleForA = (Map<String, Object>) currentForA.get("cycle");
    assertThat(newCycleForA.get("id")).isNotEqualTo(cycleId);

    // Reopening the settled cycle ("hủy tất toán") works without hitting the one-open-cycle
    // unique index, even though A's owner already has a fresh empty open cycle right now.
    Map<String, Object> reopened = post("/api/ledger/cycles/" + cycleId + "/reopen", Map.of(), tokenA);
    Map<String, Object> reopenedCycle = (Map<String, Object>) reopened.get("cycle");
    assertThat(reopenedCycle.get("status")).isEqualTo("open");
    assertThat((List<?>) reopened.get("settlements")).isNotNull();

    // A's own open cycle is now this reopened one again (the stray empty one was cleaned up).
    Map<String, Object> currentForAAfterReopen = get("/api/ledger/current", tokenA);
    Map<String, Object> currentCycleAfterReopen = (Map<String, Object>) currentForAAfterReopen.get("cycle");
    assertThat(currentCycleAfterReopen.get("id")).isEqualTo(cycleId);
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

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getList(String path, String token) {
    ResponseEntity<List> response = rest.exchange(path, HttpMethod.GET,
        new HttpEntity<>(authHeaders(token)), List.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    return response.getBody();
  }
}
