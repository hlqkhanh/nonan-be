package com.sharebill.contact;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<ContactEntity, String> {
  List<ContactEntity> findAllByOwnerUserIdOrderByCreatedAtAsc(String ownerUserId);
}
