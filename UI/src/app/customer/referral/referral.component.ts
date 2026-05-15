import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface ReferralHistory {
  id: number;
  referredPhone: string;
  status: string;
  createdOn: string;
}

@Component({
  selector: 'app-referral',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './referral.component.html',
  styleUrl: './referral.component.scss'
})
export class ReferralComponent implements OnInit {
  private http = inject(HttpClient);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);
  private base = environment.apiBaseUrl;

  referralCode = signal('');
  history = signal<ReferralHistory[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.http.get<{ referralCode: string }>(`${this.base}/referrals/my-code`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(r => this.referralCode.set(r.referralCode));

    this.http.get<ReferralHistory[]>(`${this.base}/referrals/my`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: h => { this.history.set(h); this.loading.set(false); }, error: () => this.loading.set(false) });
  }

  copyCode(): void {
    navigator.clipboard.writeText(this.referralCode()).then(() => {
      this.snackbar.open('Referral code copied!', 'Close', { duration: 2000 });
    });
  }
}
