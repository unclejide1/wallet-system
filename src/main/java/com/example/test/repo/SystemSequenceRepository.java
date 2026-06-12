package com.example.test.repo;


import com.example.test.model.SystemSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SystemSequenceRepository extends JpaRepository<SystemSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SystemSequence s WHERE s.sequenceName = :name")
    Optional<SystemSequence> findByNameForUpdate(@Param("name") String name);
}
