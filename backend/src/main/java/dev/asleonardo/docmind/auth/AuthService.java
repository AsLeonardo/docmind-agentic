package dev.asleonardo.docmind.auth;

import dev.asleonardo.docmind.config.SecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties props;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       SecurityProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
    }

    @Transactional
    public String register(String email, String password) {
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalized)) {
            throw new AuthException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        AppUser user = users.save(new AppUser(normalized, passwordEncoder.encode(password)));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public String login(String email, String password) {
        return users.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .map(this::issueToken)
                // mesma mensagem para e-mail inexistente e senha errada, para não revelar quem tem conta
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));
    }

    private String issueToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("docmind")
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(props.tokenTtl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
