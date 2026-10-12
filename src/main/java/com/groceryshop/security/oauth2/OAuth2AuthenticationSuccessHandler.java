package com.groceryshop.security.oauth2;

import com.groceryshop.entity.User;
import com.groceryshop.repository.UserRepository;
import com.groceryshop.security.JwtTokenProvider;
import com.groceryshop.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Xử lý sau khi OAuth2 đăng nhập thành công:
 * - Tạo JWT token
 * - Lấy thông tin user
 * - Redirect về frontend kèm token + user info
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String authorizedRedirectUri;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(authentication);

        if (response.isCommitted()) {
            logger.debug("Response đã committed. Không thể redirect tới " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String token = tokenProvider.generateTokenFromUserId(userPrincipal.getId());

        // Lấy thông tin user để gửi kèm
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow();
        String role = user.getRole().getName();
        String username = user.getUsername();
        String email = user.getEmail();
        String avatarUrl = user.getAvatarUrl() != null ? user.getAvatarUrl() : "";
        Long id = user.getId();

        return UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                .queryParam("token", token)
                .queryParam("id", id)
                .queryParam("username", username)
                .queryParam("email", email)
                .queryParam("role", role)
                .queryParam("avatarUrl", avatarUrl)
                .build().toUriString();
    }
}
