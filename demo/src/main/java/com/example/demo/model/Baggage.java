package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Baggage")
public class Baggage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer baggageID;

    @ManyToOne(optional = false)
    @JoinColumn(name = "BookingID", nullable = false)
    private Bookings booking;

    @Column(name = "Weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(length = 30)
    private String baggageType;

    public Integer getBaggageID() { return baggageID; }

    public Bookings getBooking() { return booking; }
    public void setBooking(Bookings booking) { this.booking = booking; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public String getBaggageType() { return baggageType; }
    public void setBaggageType(String baggageType) { this.baggageType = baggageType; }
}