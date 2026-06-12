package com.example.test.service.impl;

import com.example.test.common.exception.ApiException;
import com.example.test.model.User;
import com.example.test.model.enums.Gender;
import com.example.test.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepo userRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    @Test
    void getAllUsersCapsRequestedPageSizeAtFifty() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(createUser("usr_page123"))));

        adminUserService.getAllUsers(0, 100);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void softDeleteUserByRefUpdatesUniqueFieldsAndDeletesUser() {
        User user = createUser("usr_delete123");
        user.setEmail("owner@example.com");
        user.setPhoneNumber("08111111111");
        user.setAlternativePhoneNumber("08111111112");

        when(userRepository.findByUserRef("usr_delete123")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminUserService.softDeleteUserByRef("usr_delete123");

        assertThat(user.getEmail()).startsWith("owner@example.com_deleted_");
        assertThat(user.getPhoneNumber()).startsWith("08111111111_deleted_");
        assertThat(user.getAlternativePhoneNumber()).startsWith("08111111112_deleted_");
        verify(userRepository).delete(user);
    }

    @Test
    void softDeleteUserByRefThrowsWhenUserDoesNotExist() {
        when(userRepository.findByUserRef("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.softDeleteUserByRef("missing"))
                .isInstanceOf(ApiException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.NOT_FOUND, "Target archiving candidate records missing");
    }

    private User createUser(String userRef) {
        User user = new User();
        user.setUserRef(userRef);
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setGender(Gender.OTHER);
        user.setAddress("HQ");
        user.setStateOfOrigin("Federal");
        user.setEmail("admin@example.com");
        user.setPhoneNumber("08000000000");
        return user;
    }
}
