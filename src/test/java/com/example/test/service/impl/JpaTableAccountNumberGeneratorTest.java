package com.example.test.service.impl;

import com.example.test.model.SystemSequence;
import com.example.test.repo.SystemSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaTableAccountNumberGeneratorTest {

    @Mock
    private SystemSequenceRepository sequenceRepository;

    @InjectMocks
    private JpaTableAccountNumberGenerator generator;

    @Test
    void generateNextAccountNumberInitializesSequenceWhenMissing() {
        List<Long> persistedValues = new ArrayList<>();

        when(sequenceRepository.findByNameForUpdate("ACCOUNT_NUMBER")).thenReturn(Optional.empty());
        when(sequenceRepository.saveAndFlush(any(SystemSequence.class))).thenAnswer(invocation -> {
            SystemSequence sequence = invocation.getArgument(0);
            persistedValues.add(sequence.getNextValue());
            return sequence;
        });

        String accountNumber = generator.generateNextAccountNumber();

        assertThat(accountNumber).isEqualTo("1000000001");
        assertThat(persistedValues).containsExactly(1000000002L);
    }

    @Test
    void generateNextAccountNumberIncrementsExistingSequence() {
        SystemSequence existingSequence = new SystemSequence("ACCOUNT_NUMBER", 1000000042L);
        List<Long> persistedValues = new ArrayList<>();

        when(sequenceRepository.findByNameForUpdate("ACCOUNT_NUMBER")).thenReturn(Optional.of(existingSequence));
        when(sequenceRepository.saveAndFlush(any(SystemSequence.class))).thenAnswer(invocation -> {
            SystemSequence sequence = invocation.getArgument(0);
            persistedValues.add(sequence.getNextValue());
            return sequence;
        });

        String accountNumber = generator.generateNextAccountNumber();

        assertThat(accountNumber).isEqualTo("1000000042");
        assertThat(existingSequence.getNextValue()).isEqualTo(1000000043L);
        assertThat(persistedValues).containsExactly(1000000043L);
    }
}
