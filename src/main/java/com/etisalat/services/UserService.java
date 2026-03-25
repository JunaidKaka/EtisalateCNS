package com.etisalat.services;

import com.etisalat.dto.RoleDto;
import com.etisalat.dto.UserDto;
import com.etisalat.models.Role;
import com.etisalat.models.User;
import com.etisalat.repos.RoleRepository;
import com.etisalat.repos.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public Role saveRole(RoleDto roleDto) {
        Role role = new Role(null,roleDto.getName());
        return roleRepository.save(role);
    }

    public User saveUser(UserDto userDto) {
        if(userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setEnabled(true);


        return userRepository.save(user);
    }
}
