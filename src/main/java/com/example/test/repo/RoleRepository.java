package com.example.test.repo;


import com.example.test.model.Role;
import com.example.test.model.enums.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(AppRole name);
}