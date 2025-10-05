package info.dylansouthard.StraysBookAPI.model.friendo;

import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="litters")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
//@EqualsAndHashCode(callSuper = true, exclude = {"animals", "usersWatching", "careEvents"})
public class Litter extends Friendo {

    @OneToMany(mappedBy = "litter", cascade = CascadeType.PERSIST)
    private Set<Animal> animals = new HashSet<>();

    @OneToMany(mappedBy = "litter", cascade = CascadeType.PERSIST)
    private Set<CareEvent> careEvents = new HashSet<>();

    @ManyToMany(mappedBy = "watchedLitters")
    private Set<User> usersWatching = new HashSet<>();

    @PreRemove
    private void cleanUpRelationships() {
        for (CareEvent careEvent : careEvents) {
            careEvent.setLitter(null);
        }
        for (Animal animal : animals) {
            animal.setLitter(null);
        }

        for (User user : usersWatching) {
            user.getWatchedLitters().remove(this);
        }
    }

    public void addAnimal(Animal animal) {
        animals.add(animal);
        animal.setLitter(this);
    }

    public void addAnimals(List<Animal> animals) {
        for (Animal animal : animals) {
            this.addAnimal(animal);
        }
    }

    public void removeAnimal(Animal animal) {
        this.animals.remove(animal);
        animal.setLitter(null);
    }

    public Litter(AnimalType type, String name, List<Animal> animals) {
        super(type, name);
        this.addAnimals(animals);
    }

    public Litter(AnimalType type, String name, List<Animal> animals, GeoSchema location) {
        super(type, name, location);
        for (Animal animal : animals) {
            animal.setLocation(location);
        }
        this.addAnimals(animals);
    }

    public Litter(AnimalType type, String name) {
        super(type, name);
    }
}
