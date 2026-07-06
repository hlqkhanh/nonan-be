package com.sharebill.billtemplate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillTitleTemplateRepository extends JpaRepository<BillTitleTemplateEntity, String> {
  List<BillTitleTemplateEntity> findAllByOwnerUserIdOrderByPositionAsc(String ownerUserId);

  void deleteAllByOwnerUserId(String ownerUserId);
}
