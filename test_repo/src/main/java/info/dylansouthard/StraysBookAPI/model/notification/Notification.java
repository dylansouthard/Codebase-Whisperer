package info.dylansouthard.StraysBookAPI.model.notification;

import com.fasterxml.jackson.databind.JsonNode;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.base.UserRegisteredDBEntity;
import info.dylansouthard.StraysBookAPI.model.enums.NotificationContentType;
import info.dylansouthard.StraysBookAPI.model.enums.NotificationType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.util.helpers.JsonHelper;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "notifications")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "notification_type", discriminatorType = DiscriminatorType.STRING)
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@EqualsAndHashCode(callSuper = true, exclude = {"animals", "careEvent"})
public abstract class Notification extends UserRegisteredDBEntity {

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    protected NotificationType type = NotificationType.UPDATE;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    protected NotificationContentType contentType;

    @ManyToMany
    @JoinTable(
            name = "notification_animals",
            joinColumns = @JoinColumn(name = "notification_id"),
            inverseJoinColumns = @JoinColumn(name = "animal_id")
    )
    protected Set<Animal> animals = new HashSet<>();

    @OneToOne(mappedBy="notification")
    protected CareEvent careEvent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    protected JsonNode newValue;

    @Column(nullable = false)
    private LocalDateTime notificationDate;

    public void addAnimal(Animal animal) {
        animals.add(animal);

            animal.getAssociatedNotifications().add(this);

    }

    @Transactional
    public void removeAnimal(Animal animal) {
        this.animals.removeIf(a -> a.getId().equals(animal.getId()));
        animal.getAssociatedNotifications().removeIf(f -> f.getId().equals(this.getId()));
    }

    @PreRemove
    protected void cleanUpRelationships()  {
        for (Animal animal : animals) {
            animal.getAssociatedNotifications().remove(this);
        }
    }

    public Notification(NotificationContentType contentType, List<Animal> animals) {
        this.contentType = contentType;
        for (Animal animal : animals) {
            addAnimal(animal);
        }
    }

    public Notification(List<Animal> animals, CareEvent careEvent, User registeredBy){

        super(registeredBy);
        this.contentType = NotificationContentType.CARE_EVENT;
        for (Animal animal : animals) {
            addAnimal(animal);
        }
        this.careEvent = careEvent;
    }
    public void setNewValue(Object rawValue) {
        this.newValue = JsonHelper.convertToJsonNode(rawValue);
    }

    @PrePersist
    public void prePersist() {
        if (notificationDate == null) {
            notificationDate = createdAt != null ? createdAt : LocalDateTime.now();
        }
    }
}
