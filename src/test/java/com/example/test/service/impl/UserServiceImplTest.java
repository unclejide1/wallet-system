package com.example.test.service.impl;

import com.example.test.common.exception.ApiException;
import com.example.test.dto.UpdateProfileRequest;
import com.example.test.dto.UserResponse;
import com.example.test.model.User;
import com.example.test.model.enums.Gender;
import com.example.test.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepo userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateProfileSanitizesFieldsAndPersistsUser() {
        User user = createUser("usr_profile123", "owner@example.com", "08000000000");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("<b>Ada</b>");
        request.setLastName("<i>Lovelace</i>");
        request.setOtherName("<script>alert(1)</script>Test");
        request.setAddress("<p>12 Broad Street</p>");
        request.setPhoneNumber("08111111111");
        request.setAlternativePhoneNumber("08111111112");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByPhoneNumber("08111111111")).thenReturn(Optional.empty());
        when(userRepository.findByAlternativePhoneNumber("08111111112")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile("owner@example.com", request);

        assertThat(response.getFirstName()).isEqualTo("Ada");
        assertThat(response.getLastName()).isEqualTo("Lovelace");
        assertThat(response.getOtherName()).isEqualTo("Test");
        assertThat(response.getAddress()).isEqualTo("12 Broad Street");
        assertThat(response.getPhoneNumber()).isEqualTo("08111111111");
        assertThat(response.getAlternativePhoneNumber()).isEqualTo("08111111112");
    }

    @Test
    void getUserProfileNormalizesEmailBeforeRepositoryLookup() {
        User user = createUser("usr_profile123", "owner@example.com", "08000000000");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserProfile(" Owner@Example.com ");

        assertThat(response.getEmail()).isEqualTo("owner@example.com");
        verify(userRepository).findByEmail("owner@example.com");
    }

    @Test
    void updateProfileThrowsWhenPhoneBelongsToAnotherUser() {
        User currentUser = createUser("usr_profile123", "owner@example.com", "08000000000");
        User anotherUser = createUser("usr_profile999", "other@example.com", "08111111111");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setAddress("12 Broad Street");
        request.setPhoneNumber("08111111111");
        request.setAlternativePhoneNumber("08111111112");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByPhoneNumber("08111111111")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.updateProfile("owner@example.com", request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.BAD_REQUEST, "Primary phone number is already in use by another account");
    }

    @Test
    void updateProfileThrowsWhenAlternativePhoneBelongsToAnotherUser() {
        User currentUser = createUser("usr_profile123", "owner@example.com", "08000000000");
        User anotherUser = createUser("usr_profile999", "other@example.com", "08111111113");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setAddress("12 Broad Street");
        request.setPhoneNumber("08111111111");
        request.setAlternativePhoneNumber("08111111113");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByPhoneNumber("08111111111")).thenReturn(Optional.empty());
        when(userRepository.findByAlternativePhoneNumber("08111111113")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> userService.updateProfile(" Owner@Example.com ", request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.BAD_REQUEST, "Alternative phone number is already in use by another account");
    }

    @Test
    void updateProfileAllowsUserToRetainOwnPhoneNumbers() {
        User currentUser = createUser("usr_profile123", "owner@example.com", "08111111111");
        currentUser.setAlternativePhoneNumber("08111111112");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setOtherName("Test");
        request.setAddress("12 Broad Street");
        request.setPhoneNumber("08111111111");
        request.setAlternativePhoneNumber("08111111112");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByPhoneNumber("08111111111")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByAlternativePhoneNumber("08111111112")).thenReturn(Optional.of(currentUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile(" Owner@Example.com ", request);

        assertThat(response.getPhoneNumber()).isEqualTo("08111111111");
        assertThat(response.getAlternativePhoneNumber()).isEqualTo("08111111112");
    }

    private User createUser(String userRef, String email, String phoneNumber) {
        User user = new User();
        user.setUserRef(userRef);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setGender(Gender.FEMALE);
        user.setAddress("12 Broad Street");
        user.setStateOfOrigin("Lagos");
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        return user;
    }
}
