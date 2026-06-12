package com.example.test.service.impl;



import com.example.test.common.exception.ApiException;
import com.example.test.dto.UpdateProfileRequest;
import com.example.test.dto.UserResponse;
import com.example.test.model.User;
import com.example.test.repo.UserRepo;
import com.example.test.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepo userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User profile record not found"));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String currentEmail, UpdateProfileRequest request) {
        String normalizedEmail = normalizeEmail(currentEmail);
        log.info("Processing self-service profile updates for: {}", normalizedEmail);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User profile target not found"));

        String cleanPhone = request.getPhoneNumber().trim();
        String cleanAltPhone = request.getAlternativePhoneNumber() != null ? request.getAlternativePhoneNumber().trim() : null;

        // Verify primary phone uniqueness against other accounts
        userRepository.findByPhoneNumber(cleanPhone).ifPresent(existingUser -> {
            if (!existingUser.getUserRef().equals(user.getUserRef())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Primary phone number is already in use by another account");
            }
        });

        // Verify alternative phone uniqueness against other accounts
        if (cleanAltPhone != null && !cleanAltPhone.isBlank()) {
            userRepository.findByAlternativePhoneNumber(cleanAltPhone).ifPresent(existingUser -> {
                if (!existingUser.getUserRef().equals(user.getUserRef())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Alternative phone number is already in use by another account");
                }
            });
        }

        user.setFirstName(Jsoup.clean(request.getFirstName(), Safelist.none()));
        user.setLastName(Jsoup.clean(request.getLastName(), Safelist.none()));
        if (request.getOtherName() != null) {
            user.setOtherName(Jsoup.clean(request.getOtherName(), Safelist.none()));
        }
        user.setAddress(Jsoup.clean(request.getAddress(), Safelist.none()));
        user.setPhoneNumber(cleanPhone);
        user.setAlternativePhoneNumber(cleanAltPhone);

        return UserResponse.fromEntity(userRepository.save(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
