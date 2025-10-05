package info.dylansouthard.StraysBookAPI.model.friendo;

import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.SterilizationStatus;
import info.dylansouthard.StraysBookAPI.model.Vaccination;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.ConditionType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.enums.StatusType;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="animals")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class Animal extends Friendo {

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SexType sex = SexType.UNKNOWN;

    @Column
    private Boolean hasCollar;

    @Column
    private LocalDate born;

    @Column
    private LocalDateTime lastFed;

    @Column
    private Boolean shouldAppear = true;

    @Column
    private LocalDateTime removeAt = null;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "animal_conditions", joinColumns = @JoinColumn(name = "animal_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "conditions")
    private List<ConditionType> conditions = new ArrayList<>();

    @Column
    private Boolean criticalCondition;

    @Column
    private Integer conditionRating;

    @Column
    private String conditionNotes;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusType statusType = StatusType.STRAY;

    @Column
    private String statusNotes;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private SterilizationStatus sterilizationStatus;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Vaccination> vaccinations = new HashSet<>();

    @ManyToMany(mappedBy = "animals")
    private Set<CareEvent> careEvents = new HashSet<>();

    @ManyToMany(mappedBy = "watchedAnimals")
    private Set<User> usersWatching = new HashSet<>();

    @ManyToOne
    @JoinColumn(name="litter_id")
    private Litter litter;

    @ManyToMany(mappedBy="animals")
    private Set<Notification> associatedNotifications = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void initializeSterilizationStatus() {
        if (sterilizationStatus == null) {
            sterilizationStatus = new SterilizationStatus();
        }
    }

    @PreRemove
    private void cleanUpRelationships() {
        for (User user : usersWatching) {
            user.getWatchedAnimals().remove(this);
        }

        for (CareEvent careEvent : careEvents) {
            careEvent.getAnimals().remove(this);
        }

        for (Notification notification : associatedNotifications) {
            notification.getAnimals().remove(this);
        }

        if (litter != null) {
            litter.getAnimals().remove(this);
        }
    }

    public Animal(AnimalType type, SexType sex, String name) {
        super(type, name);
        this.sex = sex;
    }

    public Animal (AnimalType type, SexType sex, String name, GeoSchema location) {
        super(type, name, location);
        this.sex = sex;
    }

    public Animal(AnimalType type, String name) {
        super(type, name);
    }
}
