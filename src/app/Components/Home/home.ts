import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class Home {
  onLogin() {
    // TODO: Implement login functionality
    console.log('Login clicked');
  }

  onTryDemo() {
    // TODO: Implement demo functionality
    console.log('Try Demo clicked');
  }
}