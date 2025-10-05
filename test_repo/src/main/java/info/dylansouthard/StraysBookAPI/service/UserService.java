package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.dto.user.OAuthProfileDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UpdateUserDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserPrivateDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserPublicDTO;
import info.dylansouthard.StraysBookAPI.errors.AppException;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.mapper.UserMapper;
import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.OAuthProvider;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.repository.AnimalRepository;
import info.dylansouthard.StraysBookAPI.repository.AuthTokenRepository;
import info.dylansouthard.StraysBookAPI.repository.OauthProviderRepository;
import info.dylansouthard.StraysBookAPI.repository.UserRepository;
import info.dylansouthard.StraysBookAPI.rules.update.UpdateRuleLoader;
import info.dylansouthard.StraysBookAPI.rules.update.UserUpdateRule;
import info.dylansouthard.StraysBookAPI.util.updaters.UserUpdater;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private OauthProviderRepository oAuthProviderRepository;


    public User fetchOrCreateUserFromOAuthProfile(OAuthProfileDTO profile, OAuthProviderType providerType) {
        Optional<OAuthProvider> existingProvider = oAuthProviderRepository.findByProviderAndProviderUserId(providerType, profile.getId());
        User user = null;
        if (existingProvider.isPresent()) {
            user = existingProvider.get().getUser();
            if (user == null) oAuthProviderRepository.delete(existingProvider.get());
        }

        if (user == null) {
            try {
                User newUser = new User();
               if (profile.getEmail() != null) newUser.setEmail(profile.getEmail());
               newUser.setDisplayName(profile.getName());
               newUser.setProfileImgUrl(profile.getProfileImageUrl());
               newUser.addOAuthProvider(new OAuthProvider(providerType, profile.getId()));
               user = userRepository.save(newUser);
            } catch (Exception e) {
                throw ErrorFactory.internalServerError();
            }
        }
        return user;
    }

    public UserPublicDTO fetchUserDetails(Long userId) {
        User user = userRepository.findActiveById(userId).orElseThrow(ErrorFactory::userNotFound);
        return userMapper.toUserPublicDTO(user);
    }

    @Transactional
    public UserPrivateDTO fetchMyProfile(User user) {
        if (user == null) throw ErrorFactory.auth();
        User foundUser = userRepository.findActiveById(user.getId()).orElseThrow(ErrorFactory::userNotFound);
        return userMapper.toUserPrivateDTO(foundUser);
    }

    @Transactional
    public UserPrivateDTO updateUser(Long userId, UpdateUserDTO updateDTO) {
        User user = userRepository.findActiveById(userId).orElseThrow(ErrorFactory::userNotFound);

        Map<String, Object> updates = updateDTO.getUpdates();
        int numValidUpdates = 0;
        try {
            Map<String, UserUpdateRule> rules = UpdateRuleLoader.getUserRules();
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String key = entry.getKey();
                Object newValue = entry.getValue();

                UserUpdateRule rule = rules.get(key);
                if (rule == null || !rule.isValid(newValue)) continue;
                numValidUpdates++;

                UserUpdater.applyUpdate(user, key, newValue);
            }
            if (numValidUpdates == 0) throw ErrorFactory.invalidParams();
            User updatedUser = userRepository.save(user);
            return userMapper.toUserPrivateDTO(updatedUser);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw ErrorFactory.internalServerError();
        }
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findActiveById(userId).orElseThrow(ErrorFactory::userNotFound);
        try {
            user.setIsDeleted(true);
            user.setDeletionRequestedAt(LocalDateTime.now());
            userRepository.save(user);
            authTokenService.revokeAllTokensForUser(user);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw ErrorFactory.internalServerError();
        }
    }


}
