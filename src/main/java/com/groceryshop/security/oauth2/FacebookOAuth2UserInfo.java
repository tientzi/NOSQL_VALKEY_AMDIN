package com.groceryshop.security.oauth2;

import java.util.Map;

/**
 * Trích xuất thông tin user từ Facebook OAuth2 response
 */
public class FacebookOAuth2UserInfo extends OAuth2UserInfo {

    public FacebookOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getImageUrl() {
        // Facebook trả về: picture -> data -> url
        if (attributes.containsKey("picture")) {
            Object pictureObj = attributes.get("picture");
            if (pictureObj instanceof Map) {
                Map<String, Object> picture = (Map<String, Object>) pictureObj;
                if (picture.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) picture.get("data");
                    return (String) data.get("url");
                }
            }
        }
        return null;
    }
}
