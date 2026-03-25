package com.etisalat.controllers;

import com.etisalat.dto.ApiResponse;
import com.etisalat.dto.RoleDto;
import com.etisalat.dto.UserDto;
import com.etisalat.models.Role;
import com.etisalat.models.User;
import com.etisalat.services.UserService;
import com.etisalat.utils.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @PostMapping("/roles")
    public ResponseEntity<Role> createRole(@RequestBody RoleDto roleDto) {
        Role role = userService.saveRole(roleDto);
        return ResponseEntity.ok(role);
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse> createUser(@RequestBody UserDto userDto) {
        User user = userService.saveUser(userDto);
        return ResponseEntity.ok(ResponseBuilder.success(user, "User fetched"));
    }
}