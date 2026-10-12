package com.groceryshop.security.oauth2;

import com.groceryshop.entity.Role;
import com.groceryshop.entity.User;
import com.groceryshop.repository.RoleRepository;
import com.groceryshop.repository.UserRepository;
import com.groceryshop.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service xử lý thông tin user trả về từ Google/Facebook.
 * - Nếu user chưa có trong DB → tạo mới
 * - Nếu user đã có → cập nhật thông tin
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(new OAuth2Error("processing_error"), ex.getMessage(), ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                oAuth2UserRequest.getClientRegistration().getRegistrationId(),
                oAuth2User.getAttributes()
        );

        if (oAuth2UserInfo.getEmail() == null || oAuth2UserInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_found"),
                "Không tìm thấy email từ provider " + registrationId
            );
        }

        User user;
        Optional<User> userByProvider = userRepository.findByProviderAndProviderId(
                registrationId, oAuth2UserInfo.getId()
        );

        if (userByProvider.isPresent()) {
            // User đã tồn tại theo providerId → cập nhật avatar
            user = userByProvider.get();
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            // Kiểm tra xem email đã tồn tại chưa (user đăng ký local trước)
            Optional<User> userByEmail = userRepository.findByEmail(oAuth2UserInfo.getEmail());
            if (userByEmail.isPresent()) {
                // Liên kết tài khoản local với OAuth2
                user = userByEmail.get();
                user.setProvider(registrationId);
                user.setProviderId(oAuth2UserInfo.getId());
                user.setAvatarUrl(oAuth2UserInfo.getImageUrl());
                user = userRepository.save(user);
            } else {
                // Tạo user mới
                user = registerNewUser(registrationId, oAuth2UserInfo);
            }
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(String provider, OAuth2UserInfo oAuth2UserInfo) {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy ROLE_USER trong DB"));

        // Tạo username unique từ email
        String baseUsername = oAuth2UserInfo.getEmail().split("@")[0];
        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter++;
        }

        User user = User.builder()
                .username(username)
                .email(oAuth2UserInfo.getEmail())
                // OAuth2 users không có password local - dùng UUID random để thoả NOT NULL
                // Không thể login bằng password này vì không phải BCrypt hash hợp lệ
                .password("OAUTH2_" + java.util.UUID.randomUUID().toString())
                .fullName(oAuth2UserInfo.getName())
                .provider(provider)
                .providerId(oAuth2UserInfo.getId())
                .avatarUrl(oAuth2UserInfo.getImageUrl())
                .role(userRole)
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setFullName(oAuth2UserInfo.getName());
        existingUser.setAvatarUrl(oAuth2UserInfo.getImageUrl());
        return userRepository.save(existingUser);
    }
}
