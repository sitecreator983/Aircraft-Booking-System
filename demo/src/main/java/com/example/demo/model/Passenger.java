package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Passengers")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer passengerID;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Column(unique = true, length = 20)
    private String passportNumber;

    @Column(length = 100)
    private String email;

    public Integer getPassengerID() { return passengerID; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}