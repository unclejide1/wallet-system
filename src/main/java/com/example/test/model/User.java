package com.example.test.model;

import com.example.test.model.enums.Gender;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
// Overrides the default repository.delete() behavior
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
// Automatically appends "AND deleted = false" to all select queries in Hibernate 6
@SQLRestriction("deleted = false")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, length = 50)
    private String userRef;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String otherName;

    @Enumerated(EnumType.STRING) // Saves enum text ("MALE"/"FEMALE") into the table row
    @Column(nullable = false, length = 15)
    private Gender gender;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private String stateOfOrigin;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    private String alternativePhoneNumber;

    @Column(nullable = false)
    private boolean deleted = false; // The soft delete flag

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    public void generateUserRef() {
        if (this.userRef == null) {
            this.userRef = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toList());
    }

    @Override public String getUsername() { return this.email; }
    @Override public String getPassword() { return this.password; }
    @Override public boolean isAccountNonExpired() { return !deleted; } // Tied to delete flag
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return !deleted; } // Tied to delete flag
}