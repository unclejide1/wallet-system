package com.example.test.service.impl;

import com.example.test.model.SystemSequence;
import com.example.test.repo.SystemSequenceRepository;
import com.example.test.service.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaTableAccountNumberGenerator implements AccountNumberGenerator {

    private final SystemSequenceRepository sequenceRepository;
    private static final String ACCOUNT_NUM_SEQ_KEY = "ACCOUNT_NUMBER";
    private static final Long INITIAL_ACCOUNT_NUMBER = 1000000001L;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized String generateNextAccountNumber() {
        SystemSequence sequence = sequenceRepository.findByNameForUpdate(ACCOUNT_NUM_SEQ_KEY)
                .orElse(null);

        if (sequence == null) {
            log.info("Initializing system account number sequence table registry row...");
            try {
                sequenceRepository.saveAndFlush(new SystemSequence(ACCOUNT_NUM_SEQ_KEY, INITIAL_ACCOUNT_NUMBER + 1));
                return String.format("%010d", INITIAL_ACCOUNT_NUMBER);
            } catch (DataIntegrityViolationException exception) {
                log.warn("Sequence row was created concurrently. Retrying locked fetch for {}.", ACCOUNT_NUM_SEQ_KEY);
                sequence = sequenceRepository.findByNameForUpdate(ACCOUNT_NUM_SEQ_KEY)
                        .orElseThrow(() -> exception);
            }
        }

        Long currentAvailableValue = sequence.getNextValue();
        sequence.setNextValue(currentAvailableValue + 1);
        sequenceRepository.saveAndFlush(sequence);

        return String.format("%010d", currentAvailableValue);
    }
}
