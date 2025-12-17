package com.medassist.repository;

import com.medassist.entity.Conversation;
import com.medassist.enums.ConversationStatus;
import com.medassist.enums.TriageLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID>, JpaSpecificationExecutor<Conversation> {

    List<Conversation> findByClinicIdOrderByLastMessageAtDesc(UUID clinicId);

    List<Conversation> findByClinicIdAndStatusOrderByLastMessageAtDesc(UUID clinicId, ConversationStatus status);

    List<Conversation> findByClinicIdAndTriageLevelOrderByLastMessageAtDesc(UUID clinicId, TriageLevel triageLevel);

    @Query("SELECT c FROM Conversation c WHERE c.clinic.id = :clinicId AND c.patient.id = :patientId ORDER BY c.createdAt DESC")
    List<Conversation> findByClinicIdAndPatientId(@Param("clinicId") UUID clinicId, @Param("patientId") UUID patientId);

    @Query("SELECT c FROM Conversation c LEFT JOIN FETCH c.messages WHERE c.id = :id AND c.clinic.id = :clinicId")
    Optional<Conversation> findByIdAndClinicIdWithMessages(@Param("id") UUID id, @Param("clinicId") UUID clinicId);

    Optional<Conversation> findByIdAndClinicId(UUID id, UUID clinicId);
}
