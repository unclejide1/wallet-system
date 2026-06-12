package com.example.test.repo;

import com.example.test.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserRef(String userRef);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByAlternativePhoneNumber(String alternativePhoneNumber);

    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByAlternativePhoneNumber(String alternativePhoneNumber);
}
