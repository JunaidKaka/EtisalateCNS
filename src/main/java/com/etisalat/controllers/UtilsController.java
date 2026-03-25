package com.etisalat.controllers;

import com.etisalat.dto.ApiResponse;
import com.etisalat.models.Account;
import com.etisalat.models.Project;
import com.etisalat.models.User;
import com.etisalat.repos.AccountRepo;
import com.etisalat.repos.ProjectRepository;
import com.etisalat.repos.UserRepository;
import com.etisalat.utils.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/utils")
@RequiredArgsConstructor
public class UtilsController {

    private final AccountRepo accountRepo;

    private final UserRepository userRepository;

    private final ProjectRepository projectRepository;



    @GetMapping("/user/accounts")
    public ResponseEntity<ApiResponse> getAccountsByLoginUser(Authentication auth) {

        String username = getUsername(auth);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        List<Account> accountList = accountRepo.findAccountByUserId(user);

        return ResponseEntity.ok(
                ResponseBuilder.success(
                        accountList,
                        "Account fetched successfully"
                )
        );
    }

    @GetMapping("/systems")
    public ResponseEntity<ApiResponse> getSystemNames(Authentication auth) {

        List<Project> accountList = projectRepository.findAll();

        return ResponseEntity.ok(
                ResponseBuilder.success(
                        accountList,
                        "Account fetched successfully"
                )
        );
    }


    private String getUsername(Authentication auth) {
        return auth == null ? null : auth.getName();
    }
}
