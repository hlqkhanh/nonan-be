package com.sharebill.contact;

import com.sharebill.user.UserEntity;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {
  private final ContactService contactService;

  public ContactController(ContactService contactService) {
    this.contactService = contactService;
  }

  @GetMapping
  public List<ContactDto> list(@AuthenticationPrincipal UserEntity user) {
    return contactService.list(user.getId());
  }

  @PostMapping
  public ContactDto create(@Valid @RequestBody SaveContactRequest request, @AuthenticationPrincipal UserEntity user) {
    return contactService.create(user.getId(), request);
  }

  @PatchMapping("/{contactId}")
  public ContactDto update(@PathVariable String contactId, @Valid @RequestBody SaveContactRequest request,
      @AuthenticationPrincipal UserEntity user) {
    return contactService.update(user.getId(), contactId, request);
  }

  @DeleteMapping("/{contactId}")
  public ResponseEntity<Void> delete(@PathVariable String contactId, @AuthenticationPrincipal UserEntity user) {
    contactService.delete(user.getId(), contactId);
    return ResponseEntity.noContent().build();
  }
}
