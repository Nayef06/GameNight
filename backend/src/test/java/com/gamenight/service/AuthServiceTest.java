package com.gamenight.service;

import com.gamenight.dto.AuthDtos.AuthRequest;
import com.gamenight.dto.AuthDtos.AuthResponse;
import com.gamenight.model.AppUser;
import com.gamenight.repository.UserRepository;
import com.gamenight.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void userRegistrationWorks() {
        when(userRepository.existsByUsername("nayef")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed-password");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.createToken("nayef")).thenReturn("token");

        AuthResponse response = authService.register(new AuthRequest(" Nayef ", "secret1"));

        assertThat(response).isEqualTo(new AuthResponse("token", "nayef"));
        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    void duplicateUsernamesAreRejected() {
        when(userRepository.existsByUsername("nayef")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new AuthRequest("Nayef", "secret1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username already exists.");
        verify(userRepository, never()).save(any());
    }
}
