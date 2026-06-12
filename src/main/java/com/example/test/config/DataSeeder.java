package com.example.test.config;




import com.example.test.model.Role;
import com.example.test.model.User;
import com.example.test.model.enums.AppRole;
import com.example.test.model.enums.Gender;
import com.example.test.repo.RoleRepository;
import com.example.test.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Startup DataSeeder initiated. Checking system baseline definitions...");
        seedRoles();
        seedDefaultAdmin();
        log.info("Startup DataSeeder execution cycle successfully wrapped up.");
    }

    private void seedRoles() {
        Arrays.stream(AppRole.values()).forEach(roleEnum -> {
            if (roleRepository.findByName(roleEnum).isEmpty()) {
                log.info("System role '{}' absent. Seeding record into DB...", roleEnum);
                roleRepository.save(new Role(null, roleEnum));
            }
        });
    }

    private void seedDefaultAdmin() {
        String adminEmail = "admin@wallet.com";

        // Check if an admin account already exists
        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("Primary system administrator account missing. Provisioning default profile...");

            Role adminRole = roleRepository.findByName(AppRole.ADMIN)
                    .orElseThrow(() -> new IllegalStateException("ADMIN role context not found during seeding setup"));

            User admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setOtherName("Root");
            admin.setGender(Gender.OTHER);
            admin.setAddress("HQ Secure Server Core, Lagos, Nigeria");
            admin.setStateOfOrigin("Federal");
            admin.setEmail(adminEmail);
            admin.setPhoneNumber("08000000000"); // Standard unique fallback
            admin.setAlternativePhoneNumber("07000000000");
            admin.setPassword(passwordEncoder.encode("AdminPassword2026!")); // Hash securely
            admin.setRoles(Collections.singleton(adminRole));

            userRepository.save(admin);
            log.info(">> Default Admin created successfully. Credentials: [{}] / [AdminPassword2026!]", adminEmail);
        } else {
            log.info("System administrator account verified. Skipping provisioning step.");
        }
    }
}
