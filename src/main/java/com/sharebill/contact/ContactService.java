package com.sharebill.contact;

import com.sharebill.common.ForbiddenException;
import com.sharebill.common.IdGenerator;
import com.sharebill.common.NotFoundException;
import com.sharebill.favorite.FavoriteRepository;
import com.sharebill.favorite.FavoriteTargetType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {
  private final ContactRepository contactRepository;
  private final FavoriteRepository favoriteRepository;

  public ContactService(ContactRepository contactRepository, FavoriteRepository favoriteRepository) {
    this.contactRepository = contactRepository;
    this.favoriteRepository = favoriteRepository;
  }

  @Transactional(readOnly = true)
  public List<ContactDto> list(String ownerUserId) {
    Set<String> favoriteContactIds = favoriteRepository
        .findAllByOwnerUserIdAndTargetType(ownerUserId, FavoriteTargetType.CONTACT).stream()
        .map(favorite -> favorite.getTargetId())
        .collect(Collectors.toSet());

    return contactRepository.findAllByOwnerUserIdOrderByCreatedAtAsc(ownerUserId).stream()
        .map(contact -> toDto(contact, favoriteContactIds.contains(contact.getId())))
        .toList();
  }

  @Transactional
  public ContactDto create(String ownerUserId, SaveContactRequest request) {
    ContactEntity contact = new ContactEntity(
        IdGenerator.next("contact"), ownerUserId, request.name().trim(), normalizeAvatarUrl(request.avatarUrl()),
        Instant.now());
    contactRepository.save(contact);
    return toDto(contact, false);
  }

  @Transactional
  public ContactDto update(String ownerUserId, String contactId, SaveContactRequest request) {
    ContactEntity contact = requireOwnedContact(ownerUserId, contactId);
    contact.setName(request.name().trim());
    contact.setAvatarUrl(normalizeAvatarUrl(request.avatarUrl()));
    contactRepository.save(contact);

    boolean isFavorite = favoriteRepository
        .findByOwnerUserIdAndTargetTypeAndTargetId(ownerUserId, FavoriteTargetType.CONTACT, contactId)
        .isPresent();
    return toDto(contact, isFavorite);
  }

  @Transactional
  public void delete(String ownerUserId, String contactId) {
    ContactEntity contact = requireOwnedContact(ownerUserId, contactId);
    contactRepository.delete(contact);
    favoriteRepository.deleteByOwnerUserIdAndTargetTypeAndTargetId(ownerUserId, FavoriteTargetType.CONTACT, contactId);
  }

  private ContactEntity requireOwnedContact(String ownerUserId, String contactId) {
    ContactEntity contact = contactRepository.findById(contactId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy liên hệ"));
    if (!contact.getOwnerUserId().equals(ownerUserId)) {
      throw new ForbiddenException("Bạn không có quyền với liên hệ này");
    }
    return contact;
  }

  private String normalizeAvatarUrl(String avatarUrl) {
    return avatarUrl != null && avatarUrl.isBlank() ? null : avatarUrl;
  }

  private ContactDto toDto(ContactEntity contact, boolean isFavorite) {
    return new ContactDto(contact.getId(), contact.getName(), contact.getAvatarUrl(), isFavorite);
  }
}
