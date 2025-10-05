package info.dylansouthard.StraysBookAPI.model.notification;

import info.dylansouthard.StraysBookAPI.model.enums.NotificationType;
import info.dylansouthard.StraysBookAPI.model.enums.PriorityType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("ALERT")
@Getter @Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AlertNotification extends Notification {
    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private PriorityType priority = PriorityType.LOW;

    private NotificationType type = NotificationType.ALERT;

    public AlertNotification (NotificationType contentType, List<Animal> animals, User registeredBy, PriorityType priority) {
        super(animals, null, registeredBy);
        
        this.priority = priority;
    }
}
