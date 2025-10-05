package info.dylansouthard.StraysBookAPI.playground;

import info.dylansouthard.StraysBookAPI.model.base.DBEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Cat extends DBEntity {

    @ManyToMany(mappedBy = "catFriends")
    private Set<Dog> dogFriends = new HashSet<>();

}
