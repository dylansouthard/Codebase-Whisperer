package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.user.AuthTokenDTO;
import info.dylansouthard.StraysBookAPI.model.user.AuthToken;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-11T16:29:08+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Homebrew)"
)
@Component
public class AuthTokenMapperImpl implements AuthTokenMapper {

    @Override
    public AuthTokenDTO toAuthTokenDTO(AuthToken authToken) {
        if ( authToken == null ) {
            return null;
        }

        AuthTokenDTO authTokenDTO = new AuthTokenDTO();

        authTokenDTO.setType( authToken.getType() );
        authTokenDTO.setToken( authToken.getToken() );
        authTokenDTO.setIssuedAt( authToken.getIssuedAt() );
        authTokenDTO.setExpiresAt( authToken.getExpiresAt() );

        return authTokenDTO;
    }
}
