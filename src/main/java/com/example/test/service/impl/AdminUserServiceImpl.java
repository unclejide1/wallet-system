package com.example.test.service.impl;


import com.example.test.common.exception.ApiException;
import com.example.test.dto.UserResponse;
import com.example.test.model.User;
import com.example.test.repo.UserRepo;
import com.example.test.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepo userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(int page, int size) {
        log.info("Administrative fetch executed: Compiling registry list at page position: {}", page);
        if (size > 50) size = 50; // Hard memory ceiling limit to safeguard against heavy requests
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return userRepository.findAll(pageable).map(UserResponse::fromEntity);
    }

    @Override
    @Transactional
    public void softDeleteUserByRef(String userRef) {
        log.warn("Administrative action: Initiating archival soft-delete sequence on userRef token: {}", userRef);
        User user = userRepository.findByUserRef(userRef)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target archiving candidate records missing"));

        String suffix = "_deleted_" + System.currentTimeMillis();
        user.setEmail(user.getEmail() + suffix);
        user.setPhoneNumber(user.getPhoneNumber() + suffix);
        if (user.getAlternativePhoneNumber() != null) {
            user.setAlternativePhoneNumber(user.getAlternativePhoneNumber() + suffix);
        }

        userRepository.saveAndFlush(user);
        userRepository.delete(user);
        log.info("Archival block execution complete. User reference node dropped: {}", userRef);
    }
}
