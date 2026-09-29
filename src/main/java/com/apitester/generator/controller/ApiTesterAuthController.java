package com.apitester.generator.controller;

import com.apitester.generator.dto.ApiResponse;
import com.apitester.generator.dto.AuthDto;
import com.apitester.generator.service.ApiTesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RequestMapping({"/api/v1/auth", "/apitester/api/v1/auth"})
@RequiredArgsConstructor
public class ApiTesterAuthController {

    private final ApiTesterService apiTesterService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginData>> login(
            @RequestBody AuthDto.LoginRequest request,
            HttpServletResponse response) {
        AuthDto.LoginData data = apiTesterService.login(request);

        Cookie cookie = new Cookie("token", data.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (data.getExpiresAt() - System.currentTimeMillis() / 1000));
        response.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.success("Login successful", data));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.MeData>> me() {
        return ResponseEntity.ok(ApiResponse.success(apiTesterService.me()));
    }
}
