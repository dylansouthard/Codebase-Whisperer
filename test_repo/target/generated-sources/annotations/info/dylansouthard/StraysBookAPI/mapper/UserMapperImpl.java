package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.user.UserPrivateDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserPublicDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.model.user.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-11T16:29:08+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Homebrew)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserPrivateDTO toUserPrivateDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserPrivateDTO userPrivateDTO = new UserPrivateDTO();

        userPrivateDTO.setId( user.getId() );
        userPrivateDTO.setDisplayName( user.getDisplayName() );
        userPrivateDTO.setIntro( user.getIntro() );
        userPrivateDTO.setProfileImgUrl( user.getProfileImgUrl() );
        userPrivateDTO.setEmail( user.getEmail() );

        return userPrivateDTO;
    }

    @Override
    public UserPublicDTO toUserPublicDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserPublicDTO userPublicDTO = new UserPublicDTO();

        userPublicDTO.setId( user.getId() );
        userPublicDTO.setDisplayName( user.getDisplayName() );
        userPublicDTO.setIntro( user.getIntro() );
        userPublicDTO.setProfileImgUrl( user.getProfileImgUrl() );

        return userPublicDTO;
    }

    @Override
    public UserSummaryMinDTO toUserSummaryMinDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserSummaryMinDTO userSummaryMinDTO = new UserSummaryMinDTO();

        userSummaryMinDTO.setId( user.getId() );
        userSummaryMinDTO.setDisplayName( user.getDisplayName() );

        return userSummaryMinDTO;
    }
}
