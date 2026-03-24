package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "Seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"AircraftID", "SeatNumber"})
)
public class Seats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer seatID;

    @ManyToOne(optional = false)
    @JoinColumn(name = "AircraftID", nullable = false)
    private Aircraft aircraft;

    @Column(name = "SeatNumber", nullable = false, length = 5)
    private String seatNumber;

    @Column(name = "SeatClass", nullable = false, length = 20)
    private String seatClass;

    public Integer getSeatID() { return seatID; }
    public Aircraft getAircraft() { return aircraft; }
    public void setAircraft(Aircraft aircraft) { this.aircraft = aircraft; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getSeatClass() { return seatClass; }
    public void setSeatClass(String seatClass) { this.seatClass = seatClass; }
}