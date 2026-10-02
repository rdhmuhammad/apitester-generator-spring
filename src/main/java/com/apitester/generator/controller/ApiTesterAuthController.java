package com.apitester.generator.controller;

import com.apitester.generator.dto.ApiResponse;
import com.apitester.generator.dto.AuthDto;
import com.apitester.generator.service.ApiTesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/apitester/api/v1/auth")
@RequiredArgsConstructor
public class ApiTesterAuthController {

    private final ApiTesterService apiTesterService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginData>> login(
            @RequestBody AuthDto.LoginRequest request) {
        AuthDto.LoginData data = apiTesterService.login(request);

        ResponseCookie cookie = ResponseCookie.from("token", data.getToken())
                .httpOnly(true)
                .path("/")
                .maxAge(data.getExpiresAt() - System.currentTimeMillis() / 1000)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("Login successful", data));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.MeData>> me() {
        return ResponseEntity.ok(ApiResponse.success(apiTesterService.me()));
    }
}
