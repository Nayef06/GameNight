package com.gamenight.service;

import com.gamenight.model.AppUser;
import com.gamenight.model.Notification;
import com.gamenight.repository.NotificationRepository;
import com.gamenight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notificationtests;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "first")
class NotificationApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @MockitoBean private SteamOwnedGamesClient steamClient;

    private Long firstNotificationId;
    private Long secondNotificationId;

    @BeforeEach
    void setUp() {
        AppUser first = userRepository.save(new AppUser("first", "unused"));
        AppUser second = userRepository.save(new AppUser("second", "unused"));
        firstNotificationId = notificationRepository.save(new Notification(first, "Session created.")).getId();
        secondNotificationId = notificationRepository.save(new Notification(second, "Voting ended.")).getId();
    }

    @Test
    void listReturnsOnlyOwnNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(firstNotificationId))
                .andExpect(jsonPath("$[0].isRead").value(false));
        mockMvc.perform(get("/api/notifications").with(user("second")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(secondNotificationId));
    }

    @Test
    void markReadRejectsOtherUsersNotification() throws Exception {
        mockMvc.perform(patch("/api/notifications/{id}/read", secondNotificationId))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/notifications/{id}/read", firstNotificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
        mockMvc.perform(get("/api/notifications"))
                .andExpect(jsonPath("$[0].isRead").value(true));
    }
}
