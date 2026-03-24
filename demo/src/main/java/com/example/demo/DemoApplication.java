package com.example.demo;

import com.example.demo.auth.AppUser;
import com.example.demo.auth.AppUserRepository;
import com.example.demo.auth.AuthService;
import com.example.demo.catalog.AirlineAircraftCatalog;
import com.example.demo.model.Aircraft;
import com.example.demo.model.Airline;
import com.example.demo.model.Seats;
import com.example.demo.repository.AircraftRepository;
import com.example.demo.repository.SeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.demo.model.Pilot;
import com.example.demo.repository.PilotRepository;

import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDate;

import java.util.Map;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // ====== Seat config per model (DEFAULTS – μπορείς να τα αλλάξεις) ======
    private record SeatConfig(int first, int business, int premiumEconomy, int economy) {
        int total() { return first + business + premiumEconomy + economy; }
    }

    private static SeatConfig seatsForModel(String model) {
        if (model == null) return new SeatConfig(0, 0, 0, 0);
        String m = model.trim().toLowerCase();

        // Widebodies (defaults)
        if (m.contains("boeing 787")) return new SeatConfig(0, 42, 0, 198);
        if (m.contains("boeing 777")) return new SeatConfig(0, 40, 0, 260);
        if (m.contains("boeing 747")) return new SeatConfig(12, 52, 0, 300);
        if (m.contains("airbus a380")) return new SeatConfig(12, 80, 0, 380);
        if (m.contains("airbus a350")) return new SeatConfig(0, 42, 0, 260);
        if (m.contains("airbus a340")) return new SeatConfig(0, 42, 0, 250);
        if (m.contains("airbus a330")) return new SeatConfig(0, 36, 0, 240);

        // Narrowbodies (defaults)
        if (m.contains("airbus a319")) return new SeatConfig(0, 12, 0, 126);
        if (m.contains("airbus a320neo")) return new SeatConfig(0, 12, 0, 153);
        if (m.contains("airbus a320")) return new SeatConfig(0, 12, 0, 156);
        if (m.contains("airbus a321neo")) return new SeatConfig(0, 16, 0, 190);
        if (m.contains("airbus a321")) return new SeatConfig(0, 16, 0, 185);
        if (m.contains("boeing 737-700")) return new SeatConfig(0, 12, 0, 114);

        // fallback
        return new SeatConfig(0, 12, 0, 150);
    }

    private static boolean isWideBody(String model) {
        if (model == null) return false;
        String m = model.toLowerCase();
        return m.contains("787") || m.contains("777") || m.contains("747")
                || m.contains("a330") || m.contains("a340") || m.contains("a350") || m.contains("a380");
    }

    private static void generateSeatsForAircraft(Aircraft aircraft, SeatConfig cfg, SeatRepository seatRepo) {
        char[] letters = isWideBody(aircraft.getModel())
                ? new char[]{'A','B','C','D','E','F','G','H','K'}  // skip I
                : new char[]{'A','B','C','D','E','F'};

        int row = 1;
        row = createSeats(aircraft, seatRepo, row, cfg.first(), "First", letters);
        row = createSeats(aircraft, seatRepo, row, cfg.business(), "Business", letters);
        row = createSeats(aircraft, seatRepo, row, cfg.premiumEconomy(), "PremiumEconomy", letters);
        createSeats(aircraft, seatRepo, row, cfg.economy(), "Economy", letters);
    }

    private static int createSeats(Aircraft aircraft, SeatRepository seatRepo, int startRow, int count, String seatClass, char[] letters) {
        int row = startRow;
        int created = 0;

        while (created < count) {
            for (char letter : letters) {
                if (created >= count) break;

                Seats s = new Seats();
                s.setAircraft(aircraft);
                s.setSeatClass(seatClass);
                s.setSeatNumber(row + String.valueOf(letter));
                seatRepo.save(s);

                created++;
            }
            row++;
        }
        return row;
    }

    private static String guessManufacturer(String model) {
        if (model == null) return null;
        String m = model.toLowerCase();
        if (m.startsWith("airbus")) return "Airbus";
        if (m.startsWith("boeing")) return "Boeing";
        if (m.startsWith("atr")) return "ATR";
        if (m.startsWith("embraer")) return "Embraer";
        if (m.startsWith("dash")) return "De Havilland";
        return null;
    }

    // ====== Seed Fleet + Seats ======
    @Bean
    CommandLineRunner seedFleet(AircraftRepository aircraftRepo, SeatRepository seatRepo) {
        return args -> {
            if (aircraftRepo.count() > 0) return;

            for (Airline airline : Airline.values()) {
                for (Map.Entry<String, Integer> e : AirlineAircraftCatalog.fleetFor(airline).entrySet()) {
                    String model = e.getKey();
                    int count = e.getValue();

                    for (int i = 0; i < count; i++) {
                        Aircraft a = new Aircraft();
                        a.setAirline(airline);
                        a.setModel(model);
                        a.setManufacturer(guessManufacturer(model));
                        a.setBaseAirport("ATH");

                        SeatConfig cfg = seatsForModel(model);
                        a.setTotalSeats(cfg.total());

                        Aircraft saved = aircraftRepo.save(a);

                        // create seats only once per aircraft
                        if (seatRepo.countByAircraft_AircraftID(saved.getAircraftID()) == 0) {
                            generateSeatsForAircraft(saved, cfg, seatRepo);
                        }
                    }
                }
            }
        };
    }

    @Bean
    CommandLineRunner seedPilots(PilotRepository pilotRepo) {
        return args -> {
            if (pilotRepo.count() > 0) return;

            String[] firstNames = {
                "Giorgos","Nikos","Maria","Eleni","Katerina","Dimitris","Andreas","Sofia",
                "John","James","Emily","Olivia","Liam","Noah","Mia","Lucas",
                "Ahmed","Omar","Youssef","Fatima","Aisha","Sara","Hassan","Karim",
                "Pierre","Jean","Claire","Julien","Anna","Lena","Max","Felix"
            };
            String[] lastNames = {
                "Papadopoulos","Ioannou","Georgiou","Nikolaou","Dimitriou","Christou","Pappas","Kosta",
                "Smith","Johnson","Williams","Brown","Jones","Miller","Davis","Wilson",
                "Haddad","Khan","El-Sayed","Farouk","Nassar","Saleh","Mahmoud","Aziz",
                "Dubois","Martin","Bernard","Thomas","Schneider","Fischer","Weber","Muller"
            };
            String[] nationalities = {
                "Greek","Dutch","German","French","British","Turkish","Canadian","Australian",
                "Qatari","Emirati","Egyptian","Swedish"
            };

            for (Airline airline : Airline.values()) {
                for (int i = 1; i <= 12; i++) {
                    Pilot p = new Pilot();
                    p.setAirline(airline);

                    String fn = pick(firstNames);
                    String ln = pick(lastNames);
                    p.setFirstName(fn);
                    p.setLastName(ln);

                    // DOB: 1965-01-01 .. 2000-12-31
                    LocalDate dob = randomDate(LocalDate.of(1965, 1, 1), LocalDate.of(2000, 12, 31));
                    p.setDateOfBirth(dob);

                    // Hire date: max(2010-01-01, dob+21y) .. 2025-12-31
                    LocalDate minHire = dob.plusYears(21);
                    if (minHire.isBefore(LocalDate.of(2010, 1, 1))) minHire = LocalDate.of(2010, 1, 1);
                    LocalDate hire = randomDate(minHire, LocalDate.of(2025, 12, 31));
                    p.setHireDate(hire);

                    p.setNationality(pick(nationalities));

                    // Unique constraints: email + licenseNumber
                    String base = (fn + "." + ln).toLowerCase().replaceAll("[^a-z]", "");
                    p.setEmail(base + "." + airline.getCode().toLowerCase() + i + "@example.com");
                    p.setLicenseNumber(airline.getCode() + String.format("-%04d", i) + "-LIC");

                    // Phone
                    p.setPhone("+30 69" + (10000000 + ThreadLocalRandom.current().nextInt(0, 90000000)));

                    pilotRepo.save(p);
                }
            }
        };
    }

    private static String pick(String[] arr) {
        return arr[ThreadLocalRandom.current().nextInt(arr.length)];
    }

    private static LocalDate randomDate(LocalDate startInclusive, LocalDate endInclusive) {
        long start = startInclusive.toEpochDay();
        long end = endInclusive.toEpochDay();
        long rnd = ThreadLocalRandom.current().nextLong(start, end + 1);
        return LocalDate.ofEpochDay(rnd);
    }

    // ====== Seed Admin ======
    @Bean
    CommandLineRunner seedAdmin(AppUserRepository repo, AuthService authService) {
        return args -> repo.findByUsername("admin").orElseGet(() -> {
            String hash = authService.encoder().encode("123");
            return repo.save(new AppUser("admin", hash));
        });
    }
}