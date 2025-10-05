package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.user.AuthTokenDTO;
import info.dylansouthard.StraysBookAPI.model.user.AuthToken;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthTokenMapper {
    AuthTokenDTO toAuthTokenDTO(AuthToken authToken);
}


