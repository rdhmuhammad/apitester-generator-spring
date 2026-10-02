package com.apitester.example.controller.auth;

import com.apitester.example.config.JwtUtil;
import com.apitester.example.config.UserDetailsServiceImpl;
import com.apitester.example.dto.ApiResponse;
import com.apitester.example.dto.AuthResponse;
import com.apitester.example.dto.LoginRequest;
import com.apitester.example.dto.RegisterRequest;
import com.apitester.example.entity.User;
import com.apitester.generator.annotation.ApiTester;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@ApiTester
public class AuthController {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            var userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
                return ApiResponse.error("Invalid password");
            }
            String token = jwtUtil.generateToken(request.getUsername());
            return ApiResponse.ok("Login successful", new AuthResponse(token, request.getUsername()));
        } catch (Exception e) {
            return ApiResponse.error("Invalid username or password");
        }
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            User user = userDetailsService.register(
                    request.getEmail(), request.getPassword(), request.getEmail(), passwordEncoder);
            String token = jwtUtil.generateToken(user.getUsername());
            return ApiResponse.ok("Registration successful", new AuthResponse(token, user.getUsername()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok("Logged out", null);
    }
}
