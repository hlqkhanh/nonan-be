package com.sharebill.dev;

import com.sharebill.common.IdGenerator;
import com.sharebill.contact.ContactDto;
import com.sharebill.contact.ContactService;
import com.sharebill.contact.SaveContactRequest;
import com.sharebill.group.AddGroupMemberRequest;
import com.sharebill.group.CreateGroupRequest;
import com.sharebill.group.GroupService;
import com.sharebill.user.UserEntity;
import com.sharebill.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {
  private static final String DEMO_EMAIL = "demo@sharebill.local";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ContactService contactService;
  private final GroupService groupService;

  public DevDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
      ContactService contactService, GroupService groupService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.contactService = contactService;
    this.groupService = groupService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.existsByEmail(DEMO_EMAIL)) {
      return;
    }

    UserEntity demoUser = new UserEntity(
        IdGenerator.next("user"),
        DEMO_EMAIL,
        passwordEncoder.encode("demo1234"),
        "Demo",
        "demo",
        Instant.now()
    );
    userRepository.save(demoUser);

    ContactDto khanh = contactService.create(demoUser.getId(), new SaveContactRequest("Khanh", null));
    ContactDto kien = contactService.create(demoUser.getId(), new SaveContactRequest("Kien", null));
    ContactDto thong = contactService.create(demoUser.getId(), new SaveContactRequest("Thong", null));
    ContactDto nam = contactService.create(demoUser.getId(), new SaveContactRequest("Nam", null));

    groupService.createGroup(demoUser, new CreateGroupRequest("Hoi Ban Tron", List.of(
        new AddGroupMemberRequest("user", demoUser.getId()),
        new AddGroupMemberRequest("contact", khanh.id()),
        new AddGroupMemberRequest("contact", kien.id()),
        new AddGroupMemberRequest("contact", thong.id()),
        new AddGroupMemberRequest("contact", nam.id())
    )));
  }
}
