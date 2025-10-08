package com.enterprise.km.controller;

import com.enterprise.km.dto.ApiResponse;
import com.enterprise.km.dto.LoginRequest;
import com.enterprise.km.dto.LoginResponse;
import com.enterprise.km.model.User;
import com.enterprise.km.repository.UserRepository;
import com.enterprise.km.security.JwtUtil;
import com.enterprise.km.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Set tenant context if provided, otherwise use default
            String tenantId = loginRequest.getTenantId() != null ?
                loginRequest.getTenantId() : "default";
            TenantContext.setTenantId(tenantId);

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Get user info
            User user = userRepository.findByUsernameAndTenantTenantId(
                    loginRequest.getUsername(),
                    tenantId
                )
                .orElseThrow(() -> new BadCredentialsException("用户不存在"));

            // Generate JWT token
            String token = jwtUtil.generateToken(userDetails, tenantId);

            // Build response
            LoginResponse response = LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .tenantId(tenantId)
                .build();

            return ApiResponse.success("登录成功", response);

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("用户名或密码错误");
        } finally {
            TenantContext.clear();
        }
    }
}
