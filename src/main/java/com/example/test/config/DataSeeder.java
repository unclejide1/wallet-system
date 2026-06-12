package com.example.test.config;


import com.example.test.model.Role;
import com.example.test.model.enums.AppRole;
import com.example.test.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
    }

    private void seedRoles() {
        Arrays.stream(AppRole.values()).forEach(roleEnum -> {
            if (roleRepository.findByName(roleEnum).isEmpty()) {
                roleRepository.save(new Role(null, roleEnum));
            }
        });

        System.out.println(">> Database Seeding Complete: Enum Roles Synced with DB.");
    }
}
