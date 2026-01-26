package com.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "contact_registration_details")
@Builder
public class ContactRegistrationDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String contactPersonName;
    private String contactEmail;
    private String treadLicenseNumber;
    private String vatNumber;
    private String contactNumber;
    private String registrationNumber;
}
