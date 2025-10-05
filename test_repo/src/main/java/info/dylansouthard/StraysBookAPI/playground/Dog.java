package info.dylansouthard.StraysBookAPI.playground;

import info.dylansouthard.StraysBookAPI.model.base.DBEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Dog extends DBEntity {
    @ManyToMany
    @JoinTable(
            name = "dog_cat_friends",
            joinColumns = @JoinColumn(name = "dog_id"),
            inverseJoinColumns = @JoinColumn(name = "cat_id")
    )
    private Set<Cat> catFriends = new HashSet<>();


}
