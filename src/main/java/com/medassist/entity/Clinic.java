package com.medassist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clinics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String location;

    @Column(name = "whatsapp_enabled")
    private boolean whatsappEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL)
    private List<Patient> patients;

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL)
    private List<AdminUser> adminUsers;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
