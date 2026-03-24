import { Component, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../auth.service';

import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

// Material
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';

type FlightDto = {
  flightNumber: string;
  companyName: string;
};

type BookingDto = {
  bookingId: number;
  passengerName?: string;      // αν στο backend λέγεται αλλιώς, άλλαξέ το
  passengerEmail?: string;     // idem
  flightNumber: string;
  seatId?: number;
  seatNumber?: string;
  createdAt?: string;
  totalPrice?: number;
};

@Component({
  selector: 'app-bookings',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,

    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './bookings.component.html',
  styleUrls: ['./bookings.component.css'],
})
export class BookingsComponent {
  private http = inject(HttpClient);
  private router = inject(Router);
  private auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  loading = false;
  error = '';

  flights: FlightDto[] = [];
  bookings: BookingDto[] = [];
  filteredBookings: BookingDto[] = [];

  displayedColumns = ['bookingId', 'passenger', 'flight', 'seat', 'createdAt'];

  filterForm = this.fb.group({
    flightNumber: [''], // φίλτρο ανά πτήση
  });

  ngOnInit() {
    this.load();

    this.filterForm
      .get('flightNumber')!
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyFilter());
  }

  private load() {
    this.loading = true;
    this.error = '';

    forkJoin({
      flights: this.http.get<FlightDto[]>('/api/flights').pipe(catchError(() => of([] as FlightDto[]))),
      bookings: this.http.get<BookingDto[]>('/api/bookings').pipe(
        catchError((err) => {
          this.error = err?.error?.message || 'Failed to load bookings';
          return of([] as BookingDto[]);
        })
      ),
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.loading = false))
      )
      .subscribe(({ flights, bookings }) => {
        this.flights = flights;
        this.bookings = bookings;
        this.applyFilter();
      });
  }

  applyFilter() {
    const flightNo = (this.filterForm.value.flightNumber ?? '').trim();
    this.filteredBookings = !flightNo
      ? [...this.bookings]
      : this.bookings.filter((b) => (b.flightNumber ?? '') === flightNo);
  }

  refresh() {
    this.load();
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

  logout() {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}