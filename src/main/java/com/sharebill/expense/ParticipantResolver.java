package com.sharebill.expense;

import com.sharebill.contact.ContactRepository;
import com.sharebill.user.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Resolves a prefixed participant id ({@code user:<userId>} or {@code contact:<contactId>})
 * to a display name + avatar so expense responses don't force the frontend to look up
 * every payer/participant against friends/contacts itself.
 */
@Component
public class ParticipantResolver {
  private final UserRepository userRepository;
  private final ContactRepository contactRepository;

  public ParticipantResolver(UserRepository userRepository, ContactRepository contactRepository) {
    this.userRepository = userRepository;
    this.contactRepository = contactRepository;
  }

  public record Resolved(String name, String avatarUrl) {
  }

  public Resolved resolve(String participantId) {
    if (participantId == null) {
      return new Resolved(null, null);
    }
    if (participantId.startsWith("user:")) {
      String userId = participantId.substring("user:".length());
      return userRepository.findById(userId)
          .map(u -> new Resolved(u.getDisplayName(), u.getAvatarUrl()))
          .orElse(new Resolved("(Người dùng đã xóa)", null));
    }
    if (participantId.startsWith("contact:")) {
      String contactId = participantId.substring("contact:".length());
      return contactRepository.findById(contactId)
          .map(c -> new Resolved(c.getName(), c.getAvatarUrl()))
          .orElse(new Resolved("(Liên hệ đã xóa)", null));
    }
    return new Resolved(participantId, null);
  }
}
