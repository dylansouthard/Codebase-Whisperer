package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.dto.user.AuthTokenDTO;
import info.dylansouthard.StraysBookAPI.dto.user.AuthTokenPairDTO;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.mapper.AuthTokenMapper;
import info.dylansouthard.StraysBookAPI.model.enums.AuthTokenType;
import info.dylansouthard.StraysBookAPI.model.user.AuthToken;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.repository.AuthTokenRepository;
import info.dylansouthard.StraysBookAPI.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;

@Slf4j
@Service
public class AuthTokenService {
    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private AuthTokenMapper authTokenMapper;

    @Autowired UserRepository userRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final long ACCESS_EXPIRATION = 300 * 60;
    private final long REFRESH_EXPIRATION = 30 * 24 * 60 * 60;

    public AuthTokenDTO generateAccessToken(User user, String deviceId) {
        return generateToken(user, deviceId, AuthTokenType.ACCESS, null, ACCESS_EXPIRATION);
    }

    public AuthTokenDTO generateRefreshToken(User user, String deviceId, String previousRefreshToken) {
        return generateToken(user, deviceId, AuthTokenType.REFRESH, previousRefreshToken, REFRESH_EXPIRATION);
    }

    public AuthTokenPairDTO generateAuthTokens (User user, String deviceId) {
        AuthTokenDTO accessToken = generateAccessToken(user, deviceId);
        AuthTokenDTO refreshToken = generateRefreshToken(user, deviceId, null);
        return new AuthTokenPairDTO(accessToken, refreshToken);
    }

    public Long validateAccessToken(String jwt) {

            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

            return Long.valueOf(claims.getSubject());
    }

    public User fetchUserByAccessToken(String accessToken) {
        try {
            long userId = validateAccessToken(accessToken);
            return userRepository.findById(userId).orElse(null);
        } catch (JwtException e) {
            return null;
        }
    }

    @Transactional
    public AuthTokenPairDTO refreshAccessToken(String refreshToken, String deviceId) {
        if (refreshToken == null || deviceId == null) throw ErrorFactory.invalidParams();

        AuthToken existingToken = authTokenRepository.findRefreshToken(refreshToken, deviceId, refreshToken)
                .orElseThrow(ErrorFactory::invalidToken);

        if (existingToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ErrorFactory.invalidToken();
        }

        User user = existingToken.getUser();
        AuthTokenDTO accessTokenDTO = generateAccessToken(user, deviceId);
        AuthTokenDTO refreshTokenDTO = generateRefreshToken(user, deviceId, existingToken.getToken());
        return new AuthTokenPairDTO(accessTokenDTO, refreshTokenDTO);
    }

    @Transactional
    public void revokeAllTokensForUser(User user) {
        User fetchedUser = userRepository.findById(user.getId()).orElseThrow(ErrorFactory::auth);

        Iterator<AuthToken> iterator = fetchedUser.getAuthTokens().iterator();
        while (iterator.hasNext()) {
            AuthToken token = iterator.next();
            iterator.remove();
            authTokenRepository.delete(token);
        }

        userRepository.save(fetchedUser);
    }

    @Transactional
    public void revokeTokensForDevice(User user, String deviceId) {
        User fetchedUser = userRepository.findActiveById(user.getId()).orElseThrow(ErrorFactory::auth);

        Iterator<AuthToken> iterator = fetchedUser.getAuthTokens().iterator();
        while (iterator.hasNext()) {
            AuthToken token = iterator.next();
            if (token.getDeviceId().equals(deviceId)) {
                iterator.remove();
                authTokenRepository.delete(token);
            }
        }
        userRepository.save(fetchedUser);
    }

    private AuthTokenDTO generateToken(User user, String deviceId, AuthTokenType type, String previousRefreshToken, long expirationSeconds) {
        if (deviceId == null || user == null) throw ErrorFactory.invalidParams();

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(expirationSeconds);

        User fetchedUser = userRepository.findActiveById(user.getId()).orElseThrow(ErrorFactory::auth);

        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            fetchedUser.getAuthTokens().removeIf(token -> token.getDeviceId().equals(deviceId));

            String token = Jwts.builder()
                    .setSubject(String.valueOf(user.getId()))
                    .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
                    .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            AuthToken authToken = new AuthToken(type, token, issuedAt, expiresAt, deviceId);
            authToken.setPreviousRefreshToken(previousRefreshToken);
            fetchedUser.addAuthToken(authToken);

            userRepository.save(fetchedUser);

            return authTokenMapper.toAuthTokenDTO(authToken);
        }
        catch (Exception e) {
            log.error(e.getMessage());
            throw ErrorFactory.internalServerError();
        }
    }


}
