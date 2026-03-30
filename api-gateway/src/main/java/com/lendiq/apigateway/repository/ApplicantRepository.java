package com.lendiq.apigateway.repository;

import com.lendiq.apigateway.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, UUID> {

    // Used during onboarding — PAN uniqueness check before persist
    Optional<Applicant> findByPanHash(String panHash);

    // Fraud ring graph: all applicants sharing the same device fingerprint
    @Query("SELECT a FROM Applicant a WHERE a.deviceFp = :deviceFp AND a.id != :excludeId")
    List<Applicant> findByDeviceFpExcluding(String deviceFp, UUID excludeId);

    // Fraud ring graph: all applicants sharing the same IP hash
    List<Applicant> findByIpHash(String ipHash);

    // ML pipeline batch scoring: pull feature set for a list of applicant IDs
    @Query("SELECT a FROM Applicant a WHERE a.id IN :ids")
    List<Applicant> findAllByIdIn(List<UUID> ids);

    boolean existsByPanHash(String panHash);
}