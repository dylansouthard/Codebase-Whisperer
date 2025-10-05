package info.dylansouthard.StraysBookAPI.model.base;

import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
//@EqualsAndHashCode(callSuper = true)
public abstract class UserRegisteredDBEntity extends DBEntity {

    @Column
    protected String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="user_id")
    protected User registeredBy;

    public UserRegisteredDBEntity(User registeredBy) {
        this.registeredBy = registeredBy;
    }


    public User getRegisteredBy() {
        if (registeredBy == null || registeredBy.getIsDeleted()) return null;
        return registeredBy;
    }
}
