import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../Components/navbar/navbar';
import { Footer } from '../../Components/footer/footer';

@Component({
  selector: 'app-documentation',
  standalone: true,
  imports: [CommonModule, Navbar, Footer],
  templateUrl: './documentation.html',
  styleUrls: ['./documentation.css'],
})
export class Documentation {
  copiedId: string | null = null;
  activeId: string = '';

  async copy(text: string, id: string): Promise<void> {
    if (!text) return;
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text.trim());
      } else {
        const ta = document.createElement('textarea');
        ta.value = text.trim();
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
      }
      this.copiedId = id;
      setTimeout(() => (this.copiedId = null), 1600);
    } catch (e) {
      /* swallow */
    }
  }
}
