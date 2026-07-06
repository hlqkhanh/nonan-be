package com.sharebill.billtemplate;

import com.sharebill.common.IdGenerator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillTemplateService {
  private static final int MAX_TEMPLATES = 5;

  private final BillTitleTemplateRepository repository;

  public BillTemplateService(BillTitleTemplateRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<BillTitleTemplateDto> list(String ownerUserId) {
    return repository.findAllByOwnerUserIdOrderByPositionAsc(ownerUserId).stream()
        .map(BillTitleTemplateDto::from)
        .toList();
  }

  @Transactional
  public List<BillTitleTemplateDto> replaceAll(String ownerUserId, SaveBillTemplatesRequest request) {
    List<String> labels = request.labels() == null ? List.of() : request.labels();
    if (labels.size() > MAX_TEMPLATES) {
      throw new IllegalArgumentException("Tối đa " + MAX_TEMPLATES + " mẫu tên bill");
    }
    for (String label : labels) {
      if (label == null || label.isBlank()) {
        throw new IllegalArgumentException("Tên mẫu không được để trống");
      }
      if (label.length() > 40) {
        throw new IllegalArgumentException("Tên mẫu tối đa 40 ký tự");
      }
    }

    repository.deleteAllByOwnerUserId(ownerUserId);

    List<BillTitleTemplateEntity> entities = new ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      entities.add(new BillTitleTemplateEntity(
          IdGenerator.next("billtpl"), ownerUserId, labels.get(i).trim(), i, Instant.now()));
    }
    repository.saveAll(entities);

    return entities.stream().map(BillTitleTemplateDto::from).toList();
  }
}
