package com.example.demo.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(AppUserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        AppUser user = repo.findByUsername(req.username)
                .orElseThrow(() -> new AuthExceptions.Unauthorized("Invalid credentials"));

        if (user.isLocked()) {
            throw new AuthExceptions.Locked("Για λόγους ασφαλείας κλειδωθήκατε");
        }

        boolean ok = encoder.matches(req.password, user.getPasswordHash());
        if (ok) {
            user.setFailedAttempts(0);
            repo.save(user);
            return new LoginResponse(user.getUsername(), "OK");
        }

        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        if (attempts >= 3) {
            user.setLocked(true);
            repo.save(user);
            throw new AuthExceptions.Locked("Για λόγους ασφαλείας κλειδωθήκατε");
        }

        repo.save(user);
        throw new AuthExceptions.Unauthorized("Invalid credentials");
    }

    public BCryptPasswordEncoder encoder() {
        return encoder;
    }
}
