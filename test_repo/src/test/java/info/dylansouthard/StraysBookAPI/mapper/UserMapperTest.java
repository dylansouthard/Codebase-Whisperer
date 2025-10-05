package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.dto.user.UserPrivateDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserPublicDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.model.user.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

public class UserMapperTest {

   private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);


   @Test
   public void When_MappingToUserSummaryDTO_Then_ReturnUserSummaryDTO() {
       User user = DummyTestData.createUser();
       UserSummaryMinDTO minDTO = userMapper.toUserSummaryMinDTO(user);

       assertAll("User Summary Min Assertions",
               ()->assertNotNull(minDTO),
               ()->assertEquals(user.getDisplayName(), minDTO.getDisplayName())
       );
   }

    @Test
    public void When_MappingToUserPublicDTO_Then_ReturnUserPublicDTO() {
        User user = DummyTestData.createUser();
        user.setIntro("I love me some strays!");
        UserPublicDTO publicDTO = userMapper.toUserPublicDTO(user);

        assertAll("User Public Assertions",
                ()->assertNotNull(publicDTO),
                ()->assertEquals(user.getDisplayName(), publicDTO.getDisplayName()),
                ()->assertEquals(user.getIntro(), publicDTO.getIntro())
        );
    }

    @Test
    public void When_MappingToUserPrivateDTO_Then_ReturnUserPrivateDTO() {
        User user = DummyTestData.createUser();
        user.setIntro("I love me some strays!");
        user.setEmail("user@email.com");
        UserPrivateDTO privateDTO = userMapper.toUserPrivateDTO(user);

        assertAll("User Private Assertions",
                ()->assertNotNull(privateDTO),
                ()->assertEquals(user.getDisplayName(), privateDTO.getDisplayName()),
                ()->assertEquals(user.getIntro(), privateDTO.getIntro()),
                ()->assertEquals(user.getEmail(), privateDTO.getEmail())
        );
    }
}
