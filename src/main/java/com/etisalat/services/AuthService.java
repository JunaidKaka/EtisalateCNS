package com.etisalat.services;

import com.etisalat.models.User;
import com.etisalat.repos.RoleRepository;
import com.etisalat.repos.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public User register(String username, String password, String[] roleNames) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username exists");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRoles(roleRepository.findAll().stream()
             .filter(r -> java.util.Arrays.asList(roleNames).contains(r.getName()))
             .collect(java.util.stream.Collectors.toSet()));
        return userRepository.save(u);
    }

    public Optional<User> authenticate(String username, String password) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return Optional.empty();
        User u = opt.get();
        if (!passwordEncoder.matches(password, u.getPassword())) return Optional.empty();
        return Optional.of(u);
    }

    public String createAccessToken(User user) {
        return jwtService.generateAccessToken(user);
    }

    public String createRefreshTokenCookieValue(User user, String ip) {
        return refreshTokenService.createRefreshToken(user, ip);
    }

    public Optional<User> loadUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
