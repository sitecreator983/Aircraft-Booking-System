package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Aircraft")
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer aircraftID;

    @Column(length = 50)
    private String model;

    @Column(length = 50)
    private String manufacturer;

    private Integer totalSeats;

    @ManyToOne
    @JoinColumn(name = "Pilot1ID")
    private Pilot pilot1;

    @ManyToOne
    @JoinColumn(name = "Pilot2ID")
    private Pilot pilot2;

    @Enumerated(EnumType.STRING)
    @Column(name = "Airline", length = 30, nullable = false)
    private Airline airline;

    @Column(length = 10, nullable = false)
    private String baseAirport;

    
    public Integer getAircraftID() { return aircraftID; }

    public String getModel() { return model; }

    public void setModel(String model) { this.model = model; }

    public String getManufacturer() { return manufacturer; }

    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public Integer getTotalSeats() { return totalSeats; }

    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public Pilot getPilot1() { return pilot1; }

    public void setPilot1(Pilot pilot1) { this.pilot1 = pilot1; }

    public Pilot getPilot2() { return pilot2; }
    
    public void setPilot2(Pilot pilot2) { this.pilot2 = pilot2; }

    public Airline getAirline() { return airline; }

    public void setAirline(Airline airline) { this.airline = airline; }

    public String getBaseAirport() { return baseAirport; }
    
    public void setBaseAirport(String baseAirport) { this.baseAirport = baseAirport; }
}
