package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.user.UserPrivateDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserPublicDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserPrivateDTO toUserPrivateDTO(User user);

    UserPublicDTO toUserPublicDTO(User user);

    @Named("toUserSummaryMinDTO")
    UserSummaryMinDTO toUserSummaryMinDTO(User user);
}
