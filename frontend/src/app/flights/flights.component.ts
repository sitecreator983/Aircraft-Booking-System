import { Component, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../auth.service';

import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';

type AirlineDto = { name: string; code: string };

type FlightDto = {
  flightNumber: string;
  companyName: string;
  aircraftId: number;
  departureAirport: string;
  arrivalAirport: string;
  departureTime: string;
  arrivalTime: string;
};

@Component({
  selector: 'app-flights',
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
  templateUrl: './flights.component.html',
  styleUrl: './flights.component.css',
})
export class FlightsComponent {
  private http = inject(HttpClient);
  private router = inject(Router);
  private auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  loading = false;
  error = '';

  airlines: AirlineDto[] = [];
  flights: FlightDto[] = [];
  filteredFlights: FlightDto[] = [];

  displayedColumns = ['flightNumber', 'companyName', 'route', 'times', 'aircraftId'];

  filterForm = this.fb.group({
    companyName: [''],
  });

  ngOnInit() {
    this.load();

    this.filterForm
      .get('companyName')!
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyFilter());
  }

  private load() {
    this.loading = true;
    this.error = '';

    forkJoin({
      airlines: this.http
        .get<AirlineDto[]>('/api/airlines')
        .pipe(catchError(() => of([] as AirlineDto[]))),
      flights: this.http.get<FlightDto[]>('/api/flights').pipe(
        catchError((err) => {
          this.error = err?.error?.message || 'Failed to load flights';
          return of([] as FlightDto[]);
        })
      ),
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.loading = false))
      )
      .subscribe(({ airlines, flights }) => {
        this.airlines = airlines;
        this.flights = [...flights].sort((a, b) =>
          (b.departureTime || '').localeCompare(a.departureTime || '')
        );
        this.applyFilter();
      });
  }

  applyFilter() {
    const company = (this.filterForm.value.companyName ?? '').trim();
    this.filteredFlights = !company
      ? [...this.flights]
      : this.flights.filter((f) => (f.companyName ?? '') === company);
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

  // ✅ ΝΕΟ: για το menu item "Bookings"
  goBookings() {
    this.router.navigateByUrl('/bookings');
  }

  logout() {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}