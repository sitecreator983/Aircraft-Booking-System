import { Component, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin, of, combineLatest } from 'rxjs';
import { switchMap, catchError, startWith, map, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

// Angular Material (standalone imports)
import { MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

type AirlineDto = { name: string; code: string };

type AircraftDto = {
  aircraftID: number;
  model: string;
  manufacturer: string;
  totalSeats: number | null;
  baseAirport: string;
  pilot1Id: number | null;
  pilot2Id: number | null;
};

type AirportOption = { name: string; code: string; display: string };

type FlightDto = {
  flightNumber: string;
  companyName: string;
  aircraftID: number;
  departureAirport: string;
  arrivalAirport: string;
  departureTime: string; // ISO string
  arrivalTime: string;   // ISO string
};

type SeatDto = {
  seatID: number;
  aircraftID: number;
  seatNumber: string;
  seatClass: string;
};

type PilotDto = {
  pilotID: number;
  firstName: string;
  lastName: string;
  airline?: string | null;
};

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,

    // Material
    MatStepperModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,

    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    MatMenuModule,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);
  private snack = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  constructor(private auth: AuthService, private router: Router) {}

  loading = false;

  airlines: AirlineDto[] = [];
  companies: string[] = []; // dropdown values

  aircraft: AircraftDto[] = [];
  flights: FlightDto[] = [];
  airports: AirportOption[] = [];
  filteredAircraft: AircraftDto[] = [];

  seats: SeatDto[] = [];
  filteredSeats: SeatDto[] = [];

  createdBookingId: number | null = null;

  // ===================== PILOTS (NEW) =====================
  pilotForm: FormGroup = this.fb.group({
    pilot1Id: [null as number | null, Validators.required],
    pilot2Id: [null as number | null, Validators.required],
  });

  pilotsLoading = false;
  availablePilots: PilotDto[] = [];
  availablePilotsForPilot2: PilotDto[] = [];
  samePilotSelected = false;

  // used to avoid refetching & repatching pilots repeatedly
  private lastPrefilledFlightNumber: string | null = null;

  timeOptions: string[] = Array.from({ length: 24 * 4 }, (_, i) => {
    const h = String(Math.floor(i / 4)).padStart(2, '0');
    const m = String((i % 4) * 15).padStart(2, '0');
    return `${h}:${m}`;
  });

  passengerForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    passportNumber: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });

  flightForm = this.fb.group({
    mode: ['existing' as 'existing' | 'create', Validators.required],

    existingFlightNumber: [''],

    companyName: [''],
    flightDigits: [''],
    aircraftId: [null as number | null],
    departureAirport: [''],
    arrivalAirport: [''],

    departureDate: [null as Date | null],
    departureTime: [''],

    arrivalDate: [null as Date | null],
    arrivalTime: [''],
  });

  seatForm = this.fb.group({
    mode: ['existing' as 'existing' | 'create', Validators.required],
    seatClassFilter: ['', Validators.required],
    existingSeatId: [null as number | null],

    seatNumber: [''],
    seatClass: [''],
  });

  baggageForm = this.fb.group({
    addBaggage: [false],
    weightKg: [null as number | null],
    baggageType: [''],
  });

  ngOnInit() {
    this.reloadLookups();

    // pilotForm validation
    this.pilotForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v: any) => {
        this.samePilotSelected = !!v?.pilot1Id && !!v?.pilot2Id && v.pilot1Id === v.pilot2Id;
        this.refreshPilot2Options();
      });

    // filter aircraft by airline
    this.flightForm
      .get('companyName')!
      .valueChanges.pipe(
        startWith(this.flightForm.value.companyName ?? ''),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((company) => {
        const airline = (company ?? '').trim();

        if (!airline) {
          this.filteredAircraft = this.aircraft;
          this.flightForm.patchValue({ aircraftId: null }, { emitEvent: false });
          return;
        }

        this.http
          .get<AircraftDto[]>(`/api/aircraft?airline=${encodeURIComponent(airline)}`)
          .pipe(
            catchError(() => of([] as AircraftDto[])),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe((data) => {
            this.filteredAircraft = data;

            const selected = this.flightForm.value.aircraftId;
            if (selected && !this.filteredAircraft.some((a) => a.aircraftID === selected)) {
              this.flightForm.patchValue({ aircraftId: null }, { emitEvent: false });
            }
          });
      });

    // ===================== Load available pilots (CREATE + EXISTING) =====================
    combineLatest([
      this.flightForm.get('mode')!.valueChanges.pipe(startWith(this.flightForm.value.mode)),
      this.flightForm.get('existingFlightNumber')!.valueChanges.pipe(startWith(this.flightForm.value.existingFlightNumber)),
      this.flightForm.get('companyName')!.valueChanges.pipe(startWith(this.flightForm.value.companyName)),
      this.flightForm.get('aircraftId')!.valueChanges.pipe(startWith(this.flightForm.value.aircraftId)),
      this.flightForm.get('departureDate')!.valueChanges.pipe(startWith(this.flightForm.value.departureDate)),
      this.flightForm.get('departureTime')!.valueChanges.pipe(startWith(this.flightForm.value.departureTime)),
      this.flightForm.get('arrivalDate')!.valueChanges.pipe(startWith(this.flightForm.value.arrivalDate)),
      this.flightForm.get('arrivalTime')!.valueChanges.pipe(startWith(this.flightForm.value.arrivalTime)),
    ])
      .pipe(
        switchMap(([mode, existingFlightNumber, companyName, aircraftId, depDate, depTime, arrDate, arrTime]) => {
          this.availablePilots = [];
          this.availablePilotsForPilot2 = [];
          this.pilotsLoading = true;

          const m = mode as 'existing' | 'create';
          const flightNo = (existingFlightNumber ?? '').trim();

          // ===== EXISTING FLIGHT =====
          if (m === 'existing') {
            if (!flightNo) {
              this.pilotsLoading = false;
              this.pilotForm.patchValue({ pilot1Id: null, pilot2Id: null }, { emitEvent: false });
              this.lastPrefilledFlightNumber = null;
              return of([] as PilotDto[]);
            }

            // find selected flight from already loaded flights list
            const selected = this.flights.find((f) => f.flightNumber === flightNo);
            if (!selected) {
              this.pilotsLoading = false;
              return of([] as PilotDto[]);
            }

            const dep = selected.departureTime; // ISO string from backend
            const arr = selected.arrivalTime;
            const airlineName = (selected.companyName ?? '').trim();

            // prefill assigned pilots ONCE per flight selection
            const prefill$ =
              this.lastPrefilledFlightNumber === flightNo
                ? of(null)
                : this.http.get<any>(`/api/flights/${encodeURIComponent(flightNo)}/pilots`).pipe(
                    tap((res) => {
                      const p1 = res?.pilot1Id ?? null;
                      const p2 = res?.pilot2Id ?? null;
                      this.pilotForm.patchValue({ pilot1Id: p1, pilot2Id: p2 }, { emitEvent: false });
                      this.samePilotSelected = !!p1 && !!p2 && p1 === p2;
                      this.lastPrefilledFlightNumber = flightNo;
                    }),
                    catchError(() => {
                      // if endpoint fails, just continue (no prefill)
                      this.lastPrefilledFlightNumber = flightNo;
                      return of(null);
                    })
                  );

            return prefill$.pipe(
              switchMap(() => this.loadAvailablePilots$(airlineName, dep, arr, flightNo)),
              tap(() => (this.pilotsLoading = false)),
              catchError(() => {
                this.pilotsLoading = false;
                return of([] as PilotDto[]);
              })
            );
          }

          // ===== CREATE NEW FLIGHT (old behavior) =====
          // reset "prefill memory" because we are in create mode now
          this.lastPrefilledFlightNumber = null;

          // aircraftId is included so pilots reload when aircraft changes,
          // but we filter by companyName (airline)
          if (!companyName || !aircraftId || !depDate || !depTime || !arrDate || !arrTime) {
            this.pilotsLoading = false;
            this.pilotForm.patchValue({ pilot1Id: null, pilot2Id: null }, { emitEvent: false });
            return of([] as PilotDto[]);
          }

          // airlines list must be loaded to map name->code
          if (!this.airlines || this.airlines.length === 0) {
            this.pilotsLoading = false;
            return of([] as PilotDto[]);
          }

          const dep = this.combineToIsoLocal(depDate as Date, depTime as string);
          const arr = this.combineToIsoLocal(arrDate as Date, arrTime as string);

          return this.loadAvailablePilots$(companyName as string, dep, arr, null).pipe(
            tap(() => (this.pilotsLoading = false)),
            catchError(() => {
              this.pilotsLoading = false;
              return of([] as PilotDto[]);
            })
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((pilots) => {
        this.availablePilots = pilots ?? [];
        this.refreshPilot2Options();
      });

    // validators
    this.flightForm
      .get('mode')!
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyFlightModeValidators());
    this.applyFlightModeValidators();

    // seats loader
    this.setupSeatLoader();
  }

  // ===================== PILOTS HELPERS =====================

  onPilot1Change() {
    this.refreshPilot2Options();
  }

  private refreshPilot2Options() {
    const p1 = this.pilotForm.get('pilot1Id')?.value as number | null;
    this.availablePilotsForPilot2 = this.availablePilots.filter((p) => p.pilotID !== p1);
  }

  private loadAvailablePilots$(
    companyName: string,
    departureTime: string,
    arrivalTime: string,
    excludeFlightNumber: string | null
  ) {
    this.pilotsLoading = true;

    const company = (companyName ?? '').trim();
    const code = this.airlines.find((a) => (a.name ?? '').trim() === company)?.code;

    if (!code) {
      this.pilotsLoading = false;
      return of([] as PilotDto[]);
    }

    return this.http.get<PilotDto[]>(`/api/pilots/by-airline/${encodeURIComponent(code)}`).pipe(
      switchMap((pilots: PilotDto[]) => {
        if (!pilots || pilots.length === 0) return of([] as PilotDto[]);

        const checks = pilots.map((p) => {
          const params: any = excludeFlightNumber
            ? { departureTime, arrivalTime, excludeFlightNumber }
            : { departureTime, arrivalTime };

          return this.http
            .get<any>(`/api/pilots/${p.pilotID}/availability`, { params })
            .pipe(
              map((res) => ({ pilot: p, ok: !!res?.available })),
              catchError(() => of({ pilot: p, ok: false }))
            );
        });

        return forkJoin(checks).pipe(map((results) => results.filter((r) => r.ok).map((r) => r.pilot)));
      }),
      tap(() => (this.pilotsLoading = false)),
      catchError(() => {
        this.pilotsLoading = false;
        return of([] as PilotDto[]);
      })
    );
  }

  private selectedSeatClass(): string {
    return (this.seatForm.value.seatClassFilter ?? '').trim();
  }

  private setupSeatLoader() {
    const mode$ = this.flightForm.get('mode')!.valueChanges.pipe(
      startWith(this.flightForm.value.mode),
      map(v => v as 'existing' | 'create')
    );

    const cls$ = this.seatForm.get('seatClassFilter')!.valueChanges.pipe(
      startWith(this.seatForm.value.seatClassFilter ?? ''),
      map(v => (v ?? '').trim())
    );

    const flightNo$ = this.flightForm.get('existingFlightNumber')!.valueChanges.pipe(
      startWith(this.flightForm.value.existingFlightNumber ?? ''),
      map(v => (v ?? '').trim())
    );

    const aircraftId$ = this.flightForm.get('aircraftId')!.valueChanges.pipe(
      startWith(this.flightForm.value.aircraftId),
      map(v => v ?? null)
    );

    combineLatest([mode$, cls$, flightNo$, aircraftId$])
      .pipe(
        tap(() => {
          this.seatForm.patchValue({ existingSeatId: null }, { emitEvent: false });
          this.seats = [];
          this.filteredSeats = [];
        }),
        switchMap(([mode, cls, flightNo, aircraftId]) => {
          if (!cls) return of([] as SeatDto[]);

          // ΜΟΝΟ αν είμαστε σε existing mode χρησιμοποιούμε flightNo
          if (mode === 'existing' && flightNo) {
            return this.http
              .get<SeatDto[]>(
                `/api/flights/${encodeURIComponent(flightNo)}/available-seats?seatClass=${encodeURIComponent(cls)}`
              )
              .pipe(catchError(() => of([] as SeatDto[])));
          }

          // Σε create mode (ή αν δεν υπάρχει flightNo) χρησιμοποιούμε aircraftId
          if (aircraftId) {
            return this.http
              .get<SeatDto[]>(`/api/aircraft/${aircraftId}/seats`)
              .pipe(catchError(() => of([] as SeatDto[])));
          }

          return of([] as SeatDto[]);
        }),
        tap((data) => {
          this.seats = data;
          const clsLower = this.selectedSeatClass().toLowerCase();
          this.filteredSeats = data.filter((s) => (s.seatClass ?? '').toLowerCase() === clsLower);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
    .subscribe();
  }

  private applyFlightModeValidators() {
    const mode = this.flightForm.value.mode;

    const set = (name: string, validators: any[] = []) => {
      const c = this.flightForm.get(name);
      if (!c) return;
      c.setValidators(validators);
      c.updateValueAndValidity({ emitEvent: false });
    };

    if (mode === 'existing') {
      set('existingFlightNumber', [Validators.required]);

      set('companyName');
      set('flightDigits');
      set('aircraftId');
      set('departureAirport');
      set('arrivalAirport');
      set('departureDate');
      set('departureTime');
      set('arrivalDate');
      set('arrivalTime');
    } else {
      set('existingFlightNumber');

      set('companyName', [Validators.required]);
      set('flightDigits', [Validators.required, Validators.pattern(/^\d{3}$/)]);
      set('aircraftId', [Validators.required]);
      set('departureAirport', [Validators.required]);
      set('arrivalAirport', [Validators.required]);
      set('departureDate', [Validators.required]);
      set('departureTime', [Validators.required]);
      set('arrivalDate', [Validators.required]);
      set('arrivalTime', [Validators.required]);
    }
  }

  goNextFromFlight(stepper: any) {
    this.applyFlightModeValidators();
    this.flightForm.markAllAsTouched();
    this.flightForm.updateValueAndValidity();

    if (this.flightForm.valid) stepper.next();
    else this.snack.open('Complete all required Flight fields', 'OK', { duration: 2500 });
  }

  reloadLookups() {
    this.loading = true;

    forkJoin({
      aircraft: this.http.get<AircraftDto[]>('/api/aircraft').pipe(catchError(() => of([] as AircraftDto[]))),
      flights: this.http.get<FlightDto[]>('/api/flights').pipe(catchError(() => of([] as FlightDto[]))),
      airports: this.http.get<AirportOption[]>('/api/airports').pipe(catchError(() => of([] as AirportOption[]))),
      airlines: this.http.get<AirlineDto[]>('/api/airlines').pipe(catchError(() => of([] as AirlineDto[]))),
    }).subscribe({
      next: ({ aircraft, flights, airports, airlines }) => {
        this.aircraft = aircraft;
        this.filteredAircraft = aircraft;
        this.flights = flights;
        this.airports = airports;

        this.airlines = airlines ?? [];
        this.companies = (this.airlines ?? [])
          .map((a) => (a.name ?? '').trim())
          .filter(Boolean)
          .sort((a, b) => a.localeCompare(b));

        if (this.companies.length === 0) {
          this.snack.open('No companies loaded (check /api/airlines in Network tab)', 'OK', { duration: 4000 });
        }

        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snack.open('Failed to load lookup data', 'OK', { duration: 3000 });
      },
    });
  }

  private combineToIsoLocal(date: Date, time: string): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}T${time}:00`;
  }

  private createBaggageIfNeeded$(bookingId: number) {
    const add = !!this.baggageForm.value.addBaggage;
    if (!add) return of(null);

    const weightKg = this.baggageForm.value.weightKg;
    const baggageType = (this.baggageForm.value.baggageType ?? '').trim();

    if (weightKg == null || weightKg < 0) {
      this.snack.open('Baggage weight must be >= 0', 'OK', { duration: 3000 });
      return of(null);
    }
    if (!baggageType) {
      this.snack.open('Select baggage type', 'OK', { duration: 3000 });
      return of(null);
    }

    return this.http.post<any>(`/api/bookings/${bookingId}/baggage`, {
      weightKg,
      baggageType,
    });
  }

  submitAll() {
    if (this.passengerForm.invalid) {
      this.snack.open('Complete Passenger fields', 'OK', { duration: 2500 });
      return;
    }

    const flightMode = this.flightForm.value.mode!;
    const seatMode = this.seatForm.value.mode!;

    if (flightMode === 'existing' && !this.flightForm.value.existingFlightNumber) {
      this.snack.open('Select an existing flight', 'OK', { duration: 2500 });
      return;
    }

    if (flightMode === 'create') {
      const v = this.flightForm.value;
      if (!v.companyName || !v.flightDigits || !v.aircraftId || !v.departureAirport || !v.arrivalAirport) {
        this.snack.open('Complete Flight creation fields', 'OK', { duration: 2500 });
        return;
      }
      if (!v.departureDate || !v.departureTime || !v.arrivalDate || !v.arrivalTime) {
        this.snack.open('Select departure/arrival date & time', 'OK', { duration: 2500 });
        return;
      }
      if (!/^\d{3}$/.test(v.flightDigits)) {
        this.snack.open('flightDigits must be exactly 3 digits', 'OK', { duration: 2500 });
        return;
      }
    }

    // pilots are required for BOTH create & existing (because you want to view/change them)
    if (this.pilotForm.invalid || this.samePilotSelected) {
      this.snack.open('Select two different available pilots', 'OK', { duration: 2500 });
      return;
    }

    if (seatMode === 'existing') {
      if (!this.selectedSeatClass()) {
        this.snack.open('Select a class first', 'OK', { duration: 2500 });
        return;
      }
      if (!this.seatForm.value.existingSeatId) {
        this.snack.open('Select a seat', 'OK', { duration: 2500 });
        return;
      }
    }

    if (seatMode === 'create') {
      const s = this.seatForm.value;
      if (!s.seatNumber || !s.seatClass) {
        this.snack.open('Complete Seat creation fields', 'OK', { duration: 2500 });
        return;
      }
    }

    this.loading = true;
    this.createdBookingId = null;

    const passengerReq = this.passengerForm.value;
    const createPassenger$ = this.http.post<any>('/api/passengers', {
      firstName: passengerReq.firstName,
      lastName: passengerReq.lastName,
      passportNumber: passengerReq.passportNumber,
      email: passengerReq.email,
    });

    const resolveFlight$ = (passengerRes: any) => {
      // ===== EXISTING FLIGHT: update pilots on that flight before booking =====
      if (flightMode === 'existing') {
        const flightNumber = this.flightForm.value.existingFlightNumber!;
        const pilot1Id = this.pilotForm.value.pilot1Id!;
        const pilot2Id = this.pilotForm.value.pilot2Id!;

        return this.http
          .put<any>(`/api/flights/${encodeURIComponent(flightNumber)}/pilots`, { pilot1Id, pilot2Id })
          .pipe(switchMap(() => of({ passengerRes, flightNumber })));
      }

      // ===== CREATE FLIGHT (old behavior) =====
      // Assign pilots to aircraft BEFORE creating flight
      const aircraftId = this.flightForm.value.aircraftId!;
      const pilot1Id = this.pilotForm.value.pilot1Id!;
      const pilot2Id = this.pilotForm.value.pilot2Id!;

      const f = this.flightForm.value;
      const dep = this.combineToIsoLocal(f.departureDate!, f.departureTime!);
      const arr = this.combineToIsoLocal(f.arrivalDate!, f.arrivalTime!);

      return this.http
        .put<any>(`/api/aircraft/${aircraftId}`, { pilot1Id, pilot2Id })
        .pipe(
          switchMap(() =>
            this.http.post<any>('/api/flights', {
              companyName: f.companyName,
              flightDigits: f.flightDigits,
              aircraftId: f.aircraftId,
              departureAirport: f.departureAirport,
              arrivalAirport: f.arrivalAirport,
              departureTime: dep,
              arrivalTime: arr,
              aircraftModel: null,
            })
          ),
          switchMap((flightRes) => of({ passengerRes, flightNumber: flightRes.flightNumber }))
        );
    };

    const resolveSeat$ = (ctx: { passengerRes: any; flightNumber: string }) => {
      if (seatMode === 'existing') {
        return of({ ...ctx, seatId: this.seatForm.value.existingSeatId! });
      }

      const aircraftId = this.flightForm.value.aircraftId;
      if (!aircraftId) {
        this.loading = false;
        this.snack.open('Select aircraft first (to create seat)', 'OK', { duration: 3000 });
        return of(null);
      }

      const s = this.seatForm.value;
      return this.http
        .post<any>(`/api/aircraft/${aircraftId}/seats`, {
          seatNumber: s.seatNumber,
          seatClass: s.seatClass,
        })
        .pipe(switchMap((seatRes) => of({ ...ctx, seatId: seatRes.seatID })));
    };

    const createBooking$ = (ctx: any) => {
      if (!ctx) return of(null);

      const passengerId = ctx.passengerRes.passengerID;
      return this.http
        .post<any>('/api/bookings', {
          passengerId,
          flightNumber: ctx.flightNumber,
          seatId: ctx.seatId,
          bookingDate: null,
        })
        .pipe(switchMap((bookingRes) => of({ bookingRes })));
    };

    createPassenger$
      .pipe(
        switchMap((passengerRes) => resolveFlight$(passengerRes)),
        switchMap((ctx) => resolveSeat$(ctx)),
        switchMap((ctx) => createBooking$(ctx)),
        switchMap((res: any) => {
          if (!res) return of(null);

          const bookingRes = res.bookingRes; // ✅ ΠΡΟΣΘΗΚΗ
          const bookingId = bookingRes?.bookingId ?? bookingRes?.bookingID; // ✅ ΔΙΟΡΘΩΣΗ

          if (!bookingId) {
            throw new Error('Booking id missing from API response');
          }

          return this.createBaggageIfNeeded$(bookingId).pipe(
            switchMap((baggageRes) => of({ ...res, baggageRes }))
          );
        })
      )
      .subscribe({
        next: (finalCtx: any) => {
          this.loading = false;
          if (!finalCtx) return;

          this.createdBookingId = finalCtx.bookingRes.bookingId ?? finalCtx.bookingRes.bookingID;

          if (finalCtx?.baggageRes) {
            this.snack.open(`Success! BookingID = ${this.createdBookingId} (+ baggage)`, 'OK', { duration: 4000 });
          } else {
            this.snack.open(`Success! BookingID = ${this.createdBookingId}`, 'OK', { duration: 4000 });
          }

          this.reloadLookups();
        },
        error: (err) => {
          this.loading = false;
          const msg = err?.error?.message || 'Save failed';
          this.snack.open(msg, 'OK', { duration: 5000 });
        },
      });
  }

  logout() {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  goHome() {
    this.router.navigateByUrl('/home');
  }

  goFlights() {
    this.router.navigateByUrl('/flights');
  }

  goBookings() {
    this.router.navigateByUrl('/bookings');
  }
}