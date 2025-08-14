import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../Components/navbar/navbar';

@Component({
  selector: 'app-documentation',
  standalone: true,
  imports: [CommonModule, Navbar],
  templateUrl: './documentation.html',
  styleUrls: ['./documentation.css'],
})
export class Documentation {}
