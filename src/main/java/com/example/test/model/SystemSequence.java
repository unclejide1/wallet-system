package com.example.test.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "system_sequences")
public class SystemSequence {
    @Id
    @Column(length = 50)
    private String sequenceName; // e.g., "ACCOUNT_NUMBER"

    @Column(nullable = false)
    private Long nextValue;
}
