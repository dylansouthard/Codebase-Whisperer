package info.dylansouthard.StraysBookAPI.model;

import info.dylansouthard.StraysBookAPI.model.base.UserRegisteredDBEntity;
import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.friendo.Litter;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.notification.UpdateNotification;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="care_events")
@NoArgsConstructor
@Getter @Setter
@EqualsAndHashCode(callSuper = true, exclude={"animals"})
public class CareEvent extends UserRegisteredDBEntity {


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CareEventType type;

    @Column
    private Long associatedId;

    @Column
    private String newValue;

    @Column(nullable = false)
    private LocalDateTime date;


    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "care_event_animals", // Name of the join table
            joinColumns = @JoinColumn(name = "care_event_id"),
            inverseJoinColumns = @JoinColumn(name = "animal_id") // Foreign key for Animal
    )
    private Set<Animal> animals = new HashSet<>();

    @ManyToOne
    @JoinColumn(name="litter_id")
    private Litter litter;


    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "care_event_id")
    private Notification notification;


    public void createNotification() {
        this.notification = new UpdateNotification(new ArrayList<>(animals), this, this.registeredBy);
    }

    public CareEvent(CareEventType type, LocalDateTime date, User registeredBy) {
        super(registeredBy);
        this.type = type;
        this.date = date;
        registeredBy.getCareEvents().add(this);
        createNotification();
    }

    public CareEvent(CareEventType type, LocalDateTime date, User registeredBy, List<Animal> animals) {
        super(registeredBy);
        this.type = type;
        this.date = date;
        for (Animal animal : animals) {
            addAnimal(animal);
        }
        registeredBy.getCareEvents().add(this);
        createNotification();
    }


    public CareEvent(CareEventType type, User registeredBy) {
        super(registeredBy);
        this.type = type;
        this.date = LocalDateTime.now();

        registeredBy.getCareEvents().add(this);
        createNotification();
    }

    public CareEvent(CareEventType type, User registeredBy, List<Animal> animals) {
        super(registeredBy);
        this.type = type;
        for (Animal animal : animals) {
            addAnimal(animal);
        }
        createNotification();
    }

    public void addAnimal(Animal animal) {
        animals.add(animal);
        animal.getCareEvents().add(this);
    }

    public void addLitter(Litter litter) {
        this.litter = litter;
        for (Animal animal : litter.getAnimals()) {
            this.addAnimal(animal);
        }
    }

    @PreRemove
    public void removeCareEventAssociations() {
        for (Animal animal : animals) {
            animal.getCareEvents().remove(this);
        }

        if (litter != null) {
            litter.getCareEvents().remove(this);
        }

        if (notification != null) {
            notification.setCareEvent(null);
        }
    }
}
