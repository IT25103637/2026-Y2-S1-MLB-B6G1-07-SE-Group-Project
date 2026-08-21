package com.safari.wildlife.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Breaks the Customer -> User -> Customer infinite loop and hides sensitive user fields
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"customer", "password", "roles", "authorities", "hibernateLazyInitializer", "handler"})
    private User user;

    @Column(length = 80)
    private String nationality;

    @Column(name = "passport_nic", length = 50)
    private String passportOrNic;

    @Column(length = 255)
    private String address;

    @Column(name = "emergency_contact", length = 80)
    private String emergencyContact;

    public Customer() {}

    public Customer(User user, String nationality, String passportOrNic, String address, String emergencyContact) {
        this.user = user;
        this.nationality = nationality;
        this.passportOrNic = passportOrNic;
        this.address = address;
        this.emergencyContact = emergencyContact;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getPassportOrNic() {
        return passportOrNic;
    }

    public void setPassportOrNic(String passportOrNic) {
        this.passportOrNic = passportOrNic;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}