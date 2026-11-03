package com.gamenight.service;

import com.gamenight.model.AppUser;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GroupMembership;
import com.gamenight.model.Notification;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private GroupMembershipRepository membershipRepository;
    @Mock private NotificationRepository notificationRepository;

    private NotificationService service;
    private AppUser creator;
    private AppUser member;
    private GameGroup group;

    @BeforeEach
    void setUp() {
        service = new NotificationService(currentUserService, membershipRepository,
                notificationRepository);
        creator = mock(AppUser.class);
        member = mock(AppUser.class);
        group = mock(GameGroup.class);
    }

    @Test
    void creationNotifiesOtherCurrentMembersOnly() {
        when(group.getId()).thenReturn(10L);
        when(group.getName()).thenReturn("Friday Night Group");
        when(creator.getId()).thenReturn(1L);
        when(member.getId()).thenReturn(2L);
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L))
                .thenReturn(List.of(new GroupMembership(creator, group),
                        new GroupMembership(member, group)));

        service.notifySessionCreated(group, creator);

        ArgumentCaptor<List<Notification>> saved = notificationListCaptor();
        verify(notificationRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(1);
        assertThat(saved.getValue().get(0).getUser()).isSameAs(member);
        assertThat(saved.getValue().get(0).getMessage())
                .isEqualTo("A new GameNight session was created in Friday Night Group.");
    }

    @Test
    void closingNotifiesEveryCurrentMember() {
        when(group.getId()).thenReturn(10L);
        when(group.getName()).thenReturn("Friday Night Group");
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L))
                .thenReturn(List.of(new GroupMembership(creator, group),
                        new GroupMembership(member, group)));

        service.notifySessionClosed(group);

        ArgumentCaptor<List<Notification>> saved = notificationListCaptor();
        verify(notificationRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(2);
        assertThat(saved.getValue()).extracting(Notification::getUser)
                .containsExactly(creator, member);
        assertThat(saved.getValue()).allSatisfy(notification ->
                assertThat(notification.getMessage()).isEqualTo("Voting has ended in Friday Night Group."));
    }

    @Test
    void listAndMarkReadAreScopedToCurrentUser() {
        when(creator.getId()).thenReturn(1L);
        when(currentUserService.get()).thenReturn(creator);
        Notification mine = new Notification(creator, "Voting has ended in Friday Night Group.");
        when(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(List.of(mine));
        when(notificationRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(mine));

        assertThat(service.getMine()).hasSize(1);
        assertThat(service.getMine().get(0).isRead()).isFalse();
        assertThat(service.markRead(7L).isRead()).isTrue();
        assertThat(mine.isRead()).isTrue();
        verify(notificationRepository, never()).findById(7L);
    }

    @Test
    void cannotMarkAnotherUsersNotificationRead() {
        when(creator.getId()).thenReturn(1L);
        when(currentUserService.get()).thenReturn(creator);
        when(notificationRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<Notification>> notificationListCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }
}
