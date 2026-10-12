package com.groceryshop.security.oauth2;

import java.util.Map;

/**
 * Factory tạo đúng loại OAuth2UserInfo theo provider name
 */
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("facebook")) {
            return new FacebookOAuth2UserInfo(attributes);
        } else {
            throw new IllegalArgumentException("Provider OAuth2 không được hỗ trợ: " + registrationId);
        }
    }
}
