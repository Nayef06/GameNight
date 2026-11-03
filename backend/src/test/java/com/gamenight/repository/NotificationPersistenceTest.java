package com.gamenight.repository;

import com.gamenight.model.AppUser;
import com.gamenight.model.Notification;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationPersistenceTest {
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void notificationsAndReadStateRemainAfterReloadAndStayUserScoped() {
        AppUser first = userRepository.save(new AppUser("first", "hash"));
        AppUser second = userRepository.save(new AppUser("second", "hash"));
        Long firstId = notificationRepository.saveAndFlush(new Notification(first, "Session created.")).getId();
        Long secondId = notificationRepository.saveAndFlush(new Notification(second, "Voting ended.")).getId();
        entityManager.clear();

        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(first.getId()))
                .extracting(Notification::getId).containsExactly(firstId);
        assertThat(notificationRepository.findByIdAndUserId(secondId, first.getId())).isEmpty();
        Notification notification = notificationRepository.findByIdAndUserId(firstId, first.getId()).orElseThrow();
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getCreatedAt()).isNotNull();
        notification.markRead();
        notificationRepository.flush();
        entityManager.clear();

        assertThat(notificationRepository.findByIdAndUserId(firstId, first.getId()).orElseThrow().isRead())
                .isTrue();
    }
}
