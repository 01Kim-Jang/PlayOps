package com.playops.api.config;

import com.playops.api.entity.User;
import com.playops.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.equals("/api/auth/login") || path.startsWith("/api/health")) {
            return true;
        }
        // /internal/** 은 사용자 로그인 토큰이 아니라 AiJob별 콜백 토큰으로 자체 인증한다
        // (AI Runner 컨테이너는 사용자 계정이 없다). 컨트롤러/서비스에서 매번 토큰을 검증해야 한다.
        if (path.startsWith("/internal/")) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }

        User user = authService.validateToken(token);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        request.setAttribute("currentUser", user);
        return true;
    }
}
