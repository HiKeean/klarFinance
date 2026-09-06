import { Observable } from 'rxjs';
import { AuthSession, LoginCredentials } from '../entities/auth-session';

/**
 * Port: dikonsumsi oleh application layer, diimplementasikan oleh infrastructure layer.
 */
export abstract class AuthRepository {
  abstract login(credentials: LoginCredentials): Observable<AuthSession>;
}
