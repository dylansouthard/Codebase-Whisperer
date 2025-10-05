package info.dylansouthard.StraysBookAPI.model.user;

import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="oauth_providers")
@NoArgsConstructor
public class OAuthProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProviderType provider;

    @Column(nullable = false)
    private String providerUserId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public OAuthProvider(OAuthProviderType provider, String providerUserId) {
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

}
