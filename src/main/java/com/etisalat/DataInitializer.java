package com.etisalat;

import com.etisalat.models.Project;
import com.etisalat.models.Role;
import com.etisalat.models.User;
import com.etisalat.repos.ProjectRepository;
import com.etisalat.repos.RoleRepository;
import com.etisalat.repos.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final ProjectRepository projectRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // --- Initialize Roles ---
        List<String> roles = List.of("ADMIN", "DEV", "QA", "OPERATION");
        roles.forEach(r -> {
            roleRepo.findByName(r)
                    .orElseGet(() -> roleRepo.save(new Role(null, r)));
        });

        // --- Initialize Users ---
        createUserIfNotExists("admin", "123", "ADMIN");
        createUserIfNotExists("dev", "123", "DEV");
        createUserIfNotExists("qa", "123", "QA");
        createUserIfNotExists("op", "123", "OPERATION");

        // --- Initialize Projects ---
        List<String> projects = List.of("B2C", "USP", "COMS", "RTF", "CMS", "ECM", "CRMGW");
        projects.forEach(p -> {
            projectRepo.findByLabel(p)
                    .orElseGet(() -> projectRepo.save(new Project(null, p)));
        });
    }

    private void createUserIfNotExists(String username, String password, String roleName) {
        if (userRepo.findByUsername(username).isEmpty()) {
            Role role = roleRepo.findByName(roleName)
                                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRoles(Set.of(role));
            userRepo.save(user);
        }
    }
}