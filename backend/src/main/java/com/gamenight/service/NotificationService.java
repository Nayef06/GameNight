package com.gamenight.service;

import com.gamenight.dto.NotificationDtos.NotificationResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GroupMembership;
import com.gamenight.model.Notification;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class NotificationService {
    private final CurrentUserService currentUserService;
    private final GroupMembershipRepository membershipRepository;
    private final NotificationRepository notificationRepository;

    public NotificationService(CurrentUserService currentUserService,
                               GroupMembershipRepository membershipRepository,
                               NotificationRepository notificationRepository) {
        this.currentUserService = currentUserService;
        this.membershipRepository = membershipRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMine() {
        Long userId = currentUserService.get().getId();
        return notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        Long userId = currentUserService.get().getId();
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notification not found."));
        notification.markRead();
        return toResponse(notification);
    }

    public void notifySessionCreated(GameGroup group, AppUser creator) {
        notifyMembers(group, "A new GameNight session was created in " + group.getName() + ".",
                creator.getId());
    }

    public void notifySessionClosed(GameGroup group) {
        notifyMembers(group, "Voting has ended in " + group.getName() + ".", null);
    }

    private void notifyMembers(GameGroup group, String message, Long excludedUserId) {
        List<Notification> notifications = membershipRepository
                .findByGroupIdOrderByUserUsernameAsc(group.getId()).stream()
                .map(GroupMembership::getUser)
                .filter(member -> !Objects.equals(member.getId(), excludedUserId))
                .map(member -> new Notification(member, message))
                .toList();
        notificationRepository.saveAll(notifications);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getMessage(),
                notification.getCreatedAt(), notification.isRead());
    }
}
