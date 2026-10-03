import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Store } from '@ngrx/store';

import { loadCurrentUser } from './features/auth/store/auth.actions';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  private readonly store = inject(Store);

  constructor() {
    this.store.dispatch(loadCurrentUser());
  }
}