package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.common.BaseDBTest;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.friendo.Litter;
import info.dylansouthard.StraysBookAPI.model.user.User;
import org.junit.jupiter.api.BeforeEach;

public class RepositoryIT extends BaseDBTest {



    //MOCK ENTITIES
    protected Animal validAnimal;
    protected User validUser;



    protected Litter constructValidLitter() {
        return new Litter(AnimalType.CAT, "New Litter", constructValidAnimals());
    }

    @BeforeEach
    public void setUp() {
        this.validUser = new User("Bing Bong", "bing@bong.com");
        this.validAnimal = new Animal(AnimalType.CAT, SexType.UNKNOWN, "Amelie");
    }
}
