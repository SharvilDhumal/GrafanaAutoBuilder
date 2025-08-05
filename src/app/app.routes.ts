import { Routes } from '@angular/router';
import { Home } from './Components/Home/home';
import { Login } from './Components/login/login';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: '**', redirectTo: '' } // Wildcard route for 404 pages
];
