import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';


@Component({
  standalone: true,
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  attempts = 0;
  locked = false;
  message = '';

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    // ✅ φτιάχνουμε τη φόρμα εδώ (όχι σε field initializer)
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  submit() {
    if (this.locked) return;
    if (this.form.invalid) return;

    const username = this.form.get('username')?.value;
    const password = this.form.get('password')?.value;

    this.message = '';

    this.auth.login(username, password).subscribe({
      next: (res) => {
        this.auth.setLoggedIn(res.username);
        console.log('Login OK, navigating to /home');
        this.router.navigate(['/home']);
      },
      error: (err) => {
        if (err?.status === 423) {
          this.locked = true;
          this.form.disable();
          this.message = 'Για λόγους ασφαλείας κλειδωθήκατε';
          return;
        }

        this.attempts += 1;

        if (this.attempts >= 3) {
          this.locked = true;
          this.form.disable();
          this.message = 'Για λόγους ασφαλείας κλειδωθήκατε';
          return;
        }

        this.message = 'Λάθος username ή password';
      }
    });
  }

  isInvalid(name: 'username' | 'password') {
    const c = this.form.get(name);
    return !!(c && c.invalid && (c.touched || c.dirty));
  }
  
}