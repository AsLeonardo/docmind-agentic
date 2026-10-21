package dev.asleonardo.docmind.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody Credentials body) {
        return new TokenResponse(auth.register(body.email(), body.password()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody Credentials body) {
        return new TokenResponse(auth.login(body.email(), body.password()));
    }

    public record Credentials(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record TokenResponse(String token) {
    }
}
