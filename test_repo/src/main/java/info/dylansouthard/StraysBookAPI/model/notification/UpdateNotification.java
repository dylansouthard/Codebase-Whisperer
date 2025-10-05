package info.dylansouthard.StraysBookAPI.model.notification;

import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.enums.NotificationContentType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

import java.util.List;

@Entity
@DiscriminatorValue("UPDATE")
@Getter @Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UpdateNotification extends Notification {


    public UpdateNotification(NotificationContentType contentType, List<Animal> animals) {
        super(contentType, animals);
    }

    public UpdateNotification(List<Animal> animals, CareEvent careEvent, User registeredBy){
        super(animals, careEvent, registeredBy);
    }
}
