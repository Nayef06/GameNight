package com.gamenight.service;

import com.gamenight.dto.AuthDtos.AuthRequest;
import com.gamenight.dto.AuthDtos.AuthResponse;
import com.gamenight.model.AppUser;
import com.gamenight.repository.UserRepository;
import com.gamenight.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(AuthRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        AppUser user = userRepository.save(new AppUser(username, passwordEncoder.encode(request.password())));
        return new AuthResponse(jwtService.createToken(user.getUsername()), user.getUsername());
    }

    public AuthResponse login(AuthRequest request) {
        String username = normalizeUsername(request.username());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.password()));
        return new AuthResponse(jwtService.createToken(username), username);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}

