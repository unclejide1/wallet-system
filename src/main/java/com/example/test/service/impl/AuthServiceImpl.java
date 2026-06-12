package com.example.test.service.impl;



import com.example.test.common.exception.ApiException;
import com.example.test.config.security.JwtService;
import com.example.test.dto.AuthResponse;
import com.example.test.dto.LoginRequest;
import com.example.test.dto.OnboardingRequest;
import com.example.test.dto.UserResponse;
import com.example.test.model.Role;
import com.example.test.model.User;
import com.example.test.model.enums.AppRole;
import com.example.test.repo.RoleRepository;
import com.example.test.repo.UserRepo;
import com.example.test.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public UserResponse onboardUser(OnboardingRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String normalizedPhone = request.getPhoneNumber().trim();
        String normalizedAlternativePhone = request.getAlternativePhoneNumber() != null
                ? request.getAlternativePhoneNumber().trim()
                : null;

        log.info("Executing onboarding registration checks for email: {}", normalizedEmail);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email address is already registered");
        }
        if (userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Primary phone number is already associated with an account");
        }
        if (normalizedAlternativePhone != null && !normalizedAlternativePhone.isBlank()
                && userRepository.existsByAlternativePhoneNumber(normalizedAlternativePhone)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Alternative phone number is already associated with an account");
        }

        Role defaultRole = roleRepository.findByName(AppRole.USER)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Default user role configuration uninitialized"));

        User newUser = new User();
        newUser.setFirstName(Jsoup.clean(request.getFirstName(), Safelist.none()));
        newUser.setLastName(Jsoup.clean(request.getLastName(), Safelist.none()));
        if (request.getOtherName() != null) {
            newUser.setOtherName(Jsoup.clean(request.getOtherName(), Safelist.none()));
        }
        newUser.setAddress(Jsoup.clean(request.getAddress(), Safelist.none()));
        newUser.setStateOfOrigin(Jsoup.clean(request.getStateOfOrigin(), Safelist.none()));
        newUser.setGender(request.getGender());
        newUser.setEmail(normalizedEmail);
        newUser.setPhoneNumber(normalizedPhone);

        if (normalizedAlternativePhone != null) {
            newUser.setAlternativePhoneNumber(normalizedAlternativePhone);
        }

        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRoles(Collections.singleton(defaultRole));

        User savedUser = userRepository.save(newUser);
        log.info("Successfully onboarded user. Security tracking profile initialized: {}", savedUser.getUserRef());
        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        log.info("Processing session validation verification for identity path: {}", normalizedEmail);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email address or password provided");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Validated user context missing from registry"));

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .userRef(user.getUserRef())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
    }
}
