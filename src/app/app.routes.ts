import { Routes } from '@angular/router';
import { Home } from './Components/Home/home';
import { Login } from './Components/login/login';
import { SignUp } from './Components/sign-up/sign-up';
import { ForgotPassword } from './Components/forgot-password/forgot-password';
import { ResetPassword } from './Components/reset-password/reset-password';
import { Upload } from './Components/upload/upload';
import { Documentation } from './Components/documentation/documentation';
import { AdminPanel} from './Components/admin-panel/admin-panel';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: 'signup', component: SignUp },
  { path: 'forgot-password', component: ForgotPassword },
  { path: 'reset-password', component: ResetPassword },
  { path: 'upload', component: Upload },
  { path: 'docs', component: Documentation },
  { path: 'documentation', component: Documentation },
  { path: 'admin-panel', component: AdminPanel, canActivate: [adminGuard] },
];
