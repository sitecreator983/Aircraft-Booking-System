package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "Bookings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"FlightNumber", "SeatID"})
)
public class Bookings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bookingID;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PassengerID", nullable = false)
    private Passenger passenger;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SeatID", nullable = false)
    private Seats seat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "FlightNumber", nullable = false)
    private Flights flight;

    private LocalDateTime bookingDate;

    public Integer getBookingID() { return bookingID; }

    public Passenger getPassenger() { return passenger; }
    public void setPassenger(Passenger passenger) { this.passenger = passenger; }

    public Seats getSeat() { return seat; }
    public void setSeat(Seats seat) { this.seat = seat; }

    public Flights getFlight() { return flight; }
    public void setFlight(Flights flight) { this.flight = flight; }

    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }
}