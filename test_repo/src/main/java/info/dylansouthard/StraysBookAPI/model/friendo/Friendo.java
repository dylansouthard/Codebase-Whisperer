package info.dylansouthard.StraysBookAPI.model.friendo;

import info.dylansouthard.StraysBookAPI.model.base.UserRegisteredDBEntity;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Getter @Setter
@MappedSuperclass
//@EqualsAndHashCode(callSuper = true)
public abstract class Friendo extends UserRegisteredDBEntity {
    @Column(name="animal_type",nullable = false)
    @Enumerated(EnumType.STRING)
    protected AnimalType type;

    @Column(nullable = false)
    protected String name;

    @Column
    protected String description;

    @Column
    private Boolean dangerous;

    @Column
    private String imgUrl;

    @Embedded
    private GeoSchema location;

    public Friendo(AnimalType type, String name) {
        this.type = type;
        this.name = name;
    }

    public Friendo(AnimalType type, String name, GeoSchema location) {
        this.type = type;
        this.name = name;
        this.location = location;
    }
}
