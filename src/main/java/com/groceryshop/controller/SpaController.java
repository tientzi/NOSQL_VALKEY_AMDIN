package com.groceryshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA Fallback Controller - phục vụ React index.html cho tất cả route không phải API
 * Giúp React Router hoạt động khi refresh trang hoặc truy cập trực tiếp URL
 */
@Controller
public class SpaController {

    /**
     * Trả về index.html cho mọi request không phải /api/** hoặc file tĩnh
     * React Router sẽ xử lý routing phía client
     */
    @RequestMapping(value = {
        "/",
        "/login",
        "/register",
        "/forgot-password",
        "/products",
        "/products/**",
        "/cart",
        "/checkout",
        "/orders",
        "/orders/**",
        "/profile",
        "/loyalty",
        "/loyalty/**",
        "/oauth2/callback",
        "/admin",
        "/admin/**",
        "/shipper",
        "/shipper/**"
    })
    public String index() {
        return "forward:/index.html";
    }
}
