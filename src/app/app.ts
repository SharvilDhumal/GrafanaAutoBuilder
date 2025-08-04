import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Home } from './Components/Home/home'; // <-- Add this import

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Home], // <-- Add Home here
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('grafana-autobuilder');
}
