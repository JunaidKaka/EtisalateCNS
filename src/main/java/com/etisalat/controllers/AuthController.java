package com.etisalat.controllers;

import com.etisalat.dto.AuthResponse;
import com.etisalat.dto.LoginRequest;
import com.etisalat.models.RefreshToken;
import com.etisalat.models.User;
import com.etisalat.services.AuthService;
import com.etisalat.services.RefreshTokenService;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshService;

    public AuthController(AuthService authService, RefreshTokenService refreshService) {
        this.authService = authService;
        this.refreshService = refreshService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        var opt = authService.authenticate(req.username(), req.password());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = opt.get();
        String access = authService.createAccessToken(user);
        String refreshCookieValue = authService.createRefreshTokenCookieValue(user, request.getRemoteAddr());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshCookieValue)
                .httpOnly(true)
                .secure(true)                       // allow HTTP in dev
                .path("/api/auth/refresh")
                .maxAge(Duration.ofMillis(2592000000L))
                .sameSite("Lax")                     // OK when frontend is same-site (see proxy below)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        AuthResponse ar = new AuthResponse(access, user.getRoles().stream().map(r -> r.getName()).toList());
        return ResponseEntity.ok(ar);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response,
                                                @CookieValue(name = "refreshToken", required = false) String refreshCookie) {

        var optToken = refreshService.verifyAndGet(refreshCookie);
        if (optToken.isEmpty()) {
            // revoke cookie
            ResponseCookie expired = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(true)
                    .path("/api/auth/refresh")
                    .maxAge(0)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshToken token = optToken.get();
        User user = token.getUser();

        String newCookieValue = refreshService.createRefreshToken(user, request.getRemoteAddr());
        refreshService.revoke(token, /*replacedBy*/ newCookieValue.split(":")[0]);

        String newAccess = authService.createAccessToken(user);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", newCookieValue)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(java.time.Duration.ofMillis(2592000000L))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        AuthResponse ar = new AuthResponse(newAccess, user.getRoles().stream().map(r -> r.getName()).toList());
        return ResponseEntity.ok(ar);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name="refreshToken", required=false) String refreshCookie, HttpServletResponse response) {
        if (refreshCookie != null) {
            var opt = refreshService.verifyAndGet(refreshCookie);
            opt.ifPresent(t -> {
                refreshService.revoke(t, null);
            });
        }
        ResponseCookie expired = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var opt = authService.loadUserByUsername(authentication.getName());
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User u = opt.get();
        return ResponseEntity.ok(java.util.Map.of(
            "username", u.getUsername(),
            "roles", u.getRoles().stream().map(r -> r.getName()).toList()
        ));
    }
}
