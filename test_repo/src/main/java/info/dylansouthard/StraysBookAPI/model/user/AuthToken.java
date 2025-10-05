package info.dylansouthard.StraysBookAPI.model.user;

import info.dylansouthard.StraysBookAPI.model.enums.AuthTokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name="auth_tokens")
@Entity
@NoArgsConstructor
public class AuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthTokenType type;

    @Column(nullable=false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private String deviceId;

    @Column(name = "previous_refresh_token", nullable = true)
    private String previousRefreshToken;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="user_id", nullable = false)
    private User user;


    public AuthToken(AuthTokenType type, String token, LocalDateTime issuedAt, LocalDateTime expiresAt, String deviceId) {
        this.type = type;
        this.token = token;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.deviceId = deviceId;
    }

    public AuthToken(AuthTokenType type, String token, LocalDateTime issuedAt, LocalDateTime expiresAt, User user, String deviceId) {
        this.type = type;
        this.token = token;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.user = user;
        this.deviceId = deviceId;
    }
}
