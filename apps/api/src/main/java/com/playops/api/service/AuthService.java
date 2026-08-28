package com.playops.api.service;

import com.playops.api.dto.LoginRequest;
import com.playops.api.dto.LoginResponse;
import com.playops.api.entity.User;
import com.playops.api.entity.UserSession;
import com.playops.api.exception.ApiException;
import com.playops.api.repository.UserRepository;
import com.playops.api.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final String DEFAULT_LOGIN_USERNAME = "admin";
    private static final String DEFAULT_LOGIN_PASSWORD = "admin";
    private static final int SESSION_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final String loginUsername;
    private final String loginPassword;

    public AuthService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            @Value("${LOGIN_USERNAME:" + DEFAULT_LOGIN_USERNAME + "}") String loginUsername,
            @Value("${LOGIN_PASSWORD:" + DEFAULT_LOGIN_PASSWORD + "}") String loginPassword
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginUsername = loginUsername;
        this.loginPassword = loginPassword;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            throw new ApiException(400, "Username and password are required");
        }

        User user = authenticate(request.username(), request.password());
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserSession session = new UserSession();
        session.setToken(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setExpiresAt(Instant.now().plus(SESSION_TTL_HOURS, ChronoUnit.HOURS));
        userSessionRepository.save(session);

        return new LoginResponse(session.getToken(), user.getUsername(), user.getRole());
    }

    private User authenticate(String username, String password) {
        if (loginUsername.equals(username) && loginPassword.equals(password)) {
            return userRepository.findByUsername(loginUsername)
                    .orElseGet(() -> {
                        User admin = new User();
                        admin.setUsername(loginUsername);
                        admin.setPassword(passwordEncoder.encode(loginPassword));
                        admin.setRole(com.playops.api.entity.UserRole.ADMIN);
                        return userRepository.save(admin);
                    });
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(401, "Invalid credentials"));

        // 기존에 평문으로 저장된 비밀번호와의 호환을 위해, 해시가 아닌 값은 평문 비교 후
        // 로그인 성공 시 즉시 해시로 마이그레이션한다 (별도 배치/마이그레이션 스크립트 불필요).
        if (isBcryptHash(user.getPassword())) {
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new ApiException(401, "Invalid credentials");
            }
        } else {
            if (!user.getPassword().equals(password)) {
                throw new ApiException(401, "Invalid credentials");
            }
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }

        return user;
    }

    private static boolean isBcryptHash(String value) {
        return value != null
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    /** 매 요청마다 호출되므로(AuthInterceptor), 만료된 세션은 여기서 걸러지기만 하고 별도 정리 배치는 없다 — 그냥 쌓이도록 둔다. */
    @Transactional
    public User validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        UserSession session = userSessionRepository.findByToken(token).orElse(null);
        if (session == null || session.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }
        User user = userRepository.findById(session.getUserId()).orElse(null);
        if (user == null) {
            return null;
        }
        session.setLastAccessedAt(Instant.now());
        userSessionRepository.save(session);
        return user;
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            userSessionRepository.deleteByToken(token);
        }
    }

    /** 관리자가 특정 사용자를 모든 기기에서 강제 로그아웃시킬 때 사용한다. */
    @Transactional
    public void revokeAllSessions(Long userId) {
        userSessionRepository.deleteByUserId(userId);
    }
}
