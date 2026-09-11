import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from '../../../shared/components/header/header';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, HeaderComponent, NavbarComponent],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.css'
})
export class AdminLayoutComponent {}
