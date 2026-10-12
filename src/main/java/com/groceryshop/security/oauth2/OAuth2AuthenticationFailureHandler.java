package com.groceryshop.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý khi OAuth2 đăng nhập thất bại:
 * - Redirect về trang login với thông báo lỗi ngắn gọn
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // Lấy thông báo lỗi ngắn gọn
        String message = exception.getMessage();
        if (message == null || message.length() > 100) {
            message = "Đăng nhập Google thất bại. Vui lòng thử lại.";
        }

        // Redirect về React frontend (port 5173), không phải backend (8080)
        String loginUrl = "http://localhost:5173/login"
                + "?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, loginUrl);
    }
}
