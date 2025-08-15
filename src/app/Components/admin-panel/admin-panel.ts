import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../Components/navbar/navbar';

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [CommonModule, Navbar],
  templateUrl: './admin-panel.html',
  styleUrls: ['./admin-panel.css']
})

export class AdminPanel {

}
