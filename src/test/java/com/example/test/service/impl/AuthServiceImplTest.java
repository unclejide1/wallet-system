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
import com.example.test.model.enums.Gender;
import com.example.test.repo.RoleRepository;
import com.example.test.repo.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepo userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void onboardUserCreatesSanitizedUserProfile() {
        OnboardingRequest request = new OnboardingRequest();
        request.setFirstName("<b>Ada</b>");
        request.setLastName("<i>Lovelace</i>");
        request.setOtherName("<script>alert(1)</script>Test");
        request.setGender(Gender.FEMALE);
        request.setAddress("<p>12 Broad Street</p>");
        request.setStateOfOrigin("<span>Lagos</span>");
        request.setEmail(" Ada@example.com ");
        request.setPassword("Password123!");
        request.setPhoneNumber("08111111111");
        request.setAlternativePhoneNumber("08111111112");

        Role defaultRole = new Role(1L, AppRole.USER);

        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(userRepository.existsByAlternativePhoneNumber(request.getAlternativePhoneNumber())).thenReturn(false);
        when(roleRepository.findByName(AppRole.USER)).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserRef("usr_test123456");
            return user;
        });

        UserResponse response = authService.onboardUser(request);

        assertThat(response.getUserRef()).isEqualTo("usr_test123456");
        assertThat(response.getFirstName()).isEqualTo("Ada");
        assertThat(response.getLastName()).isEqualTo("Lovelace");
        assertThat(response.getOtherName()).isEqualTo("Test");
        assertThat(response.getAddress()).isEqualTo("12 Broad Street");
        assertThat(response.getStateOfOrigin()).isEqualTo("Lagos");
        assertThat(response.getEmail()).isEqualTo("ada@example.com");
        assertThat(response.getRoles()).containsExactly("USER");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getPhoneNumber()).isEqualTo("08111111111");
        assertThat(savedUser.getAlternativePhoneNumber()).isEqualTo("08111111112");
        assertThat(savedUser.getRoles()).containsExactly(defaultRole);
    }

    @Test
    void onboardUserThrowsWhenEmailAlreadyExists() {
        OnboardingRequest request = new OnboardingRequest();
        request.setEmail("Ada@example.com");
        request.setPhoneNumber("08111111111");

        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.onboardUser(request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.BAD_REQUEST, "Email address is already registered");
    }

    @Test
    void loginNormalizesEmailBeforeAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" Ada@example.com ");
        request.setPassword("Password123!");

        User user = new User();
        user.setUserRef("usr_test123456");
        user.setEmail("ada@example.com");
        user.setRoles(Set.of(new Role(1L, AppRole.USER)));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("ada@example.com");
        assertThat(response.getRoles()).containsExactly("USER");

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getPrincipal()).isEqualTo("ada@example.com");
        assertThat(tokenCaptor.getValue().getCredentials()).isEqualTo("Password123!");
    }

    @Test
    void loginThrowsUnauthorizedWhenAuthenticationFails() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ada@example.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "message")
                .containsExactly(HttpStatus.UNAUTHORIZED, "Invalid email address or password provided");
    }
}
