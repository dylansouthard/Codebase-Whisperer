package info.dylansouthard.StraysBookAPI.model.user;

import info.dylansouthard.StraysBookAPI.errors.exceptions.DuplicateOAuthProviderException;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.base.DBEntity;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.friendo.Litter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="app_users")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class User extends DBEntity {

    @Column(nullable = false)
    @NotNull
    private String displayName;

    @Column(unique = true)
    private String email;

    @Column
    private String intro;

    @Column
    private String profileImgUrl;

    @Column
    private Boolean isDeleted = false;

    @Column
    private LocalDateTime deletionRequestedAt;

    @ManyToMany
    @JoinTable(
            name = "watched_animals", // Name of the join table
            joinColumns = @JoinColumn(name = "user_id"), // Foreign key for User
            inverseJoinColumns = @JoinColumn(name = "animal_id") // Foreign key for Animal
    )
    private Set<Animal> watchedAnimals = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "watched_litters", // Name of the join table
            joinColumns = @JoinColumn(name = "user_id"), // Foreign key for User
            inverseJoinColumns = @JoinColumn(name = "litter_id") // Foreign key for Animal
    )
    private Set<Litter> watchedLitters = new HashSet<>();

    @OneToMany(mappedBy = "registeredBy")
    private Set<CareEvent> careEvents = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OAuthProvider> oAuthProviders = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AuthToken> authTokens = new HashSet<>();

    @OneToMany(mappedBy="registeredBy")
    private Set<Notification> associatedNotifications = new HashSet<>();

    public void addOAuthProvider(OAuthProvider provider) {
        if (this.oAuthProviders.stream().anyMatch(p->p.getProvider().equals(provider.getProvider()))){
            throw new DuplicateOAuthProviderException();
        }

        this.oAuthProviders.add(provider);
        provider.setUser(this);
    }

    public void addAuthToken(AuthToken authToken) {
        Optional<AuthToken> existingToken = this.authTokens.stream()
                .filter(token -> token.getDeviceId().equals(authToken.getDeviceId())
                        && token.getType().equals(authToken.getType()))
                .findFirst();

        existingToken.ifPresent(this.authTokens::remove);
        this.authTokens.add(authToken);
        authToken.setUser(this);
    }

    @PreRemove
    private void cleanUpRelationships() {
        for (Animal animal : this.watchedAnimals) {
            animal.getUsersWatching().remove(this);
        }

        for (CareEvent careEvent : this.careEvents) {
            careEvent.setRegisteredBy(null);
        }

        for (Notification notification : this.associatedNotifications) {
            notification.setRegisteredBy(null);
        }

        for (Litter litter : this.watchedLitters) {
            litter.getUsersWatching().remove(this);
        }
    }

    public void addWatchedAnimal(Animal animal) {
        this.watchedAnimals.add(animal);
        if (!animal.getUsersWatching().contains(this)) {
            animal.getUsersWatching().add(this);
        }
    }

    public void removeWatchedAnimal(Animal animal) {
        this.watchedAnimals.removeIf(a -> a.getId().equals(animal.getId()));
        animal.getUsersWatching().removeIf(u -> u.getId().equals(this.getId()));

    }

    public void addWatchedLitter(Litter litter) {
        this.watchedLitters.add(litter);
        litter.getUsersWatching().add(this);

        for (Animal animal : litter.getAnimals()) {
            addWatchedAnimal(animal);
        }
    }

    public void removeWatchedLitter(Litter litter) {
        this.watchedLitters.removeIf(l -> l.getId().equals(litter.getId()));
        litter.getUsersWatching().removeIf(u -> u.getId().equals(this.getId()));

        for (Animal animal : litter.getAnimals()) {
            removeWatchedAnimal(animal);
        }
    }

    public User(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
}
