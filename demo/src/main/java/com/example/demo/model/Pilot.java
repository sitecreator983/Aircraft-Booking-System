package com.example.demo.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "Pilots")
public class Pilot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pilotID;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    private LocalDate dateOfBirth;

    @Column(length = 30)
    private String phone;

    @Column(unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 30)
    private String licenseNumber;

    @Column(length = 50)
    private String nationality;

    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Airline airline;

    // getters/setters
    public Integer getPilotID() { return pilotID; }

    public String getFirstName() { return firstName; }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }

    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getLicenseNumber() { return licenseNumber; }

    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getNationality() { return nationality; }

    public void setNationality(String nationality) { this.nationality = nationality; }

    public LocalDate getHireDate() { return hireDate; }
    
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public Airline getAirline() { return airline; }

    public void setAirline(Airline airline) { this.airline = airline; }
}
