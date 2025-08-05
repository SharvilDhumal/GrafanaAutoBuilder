import { Routes } from '@angular/router';
import { Home } from './Components/Home/home';
import { Login } from './Components/login/login';
import { SignUp } from './Components/sign-up/sign-up'; // <-- Add this import

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: 'signup', component: SignUp }, // <-- Add this route
];
