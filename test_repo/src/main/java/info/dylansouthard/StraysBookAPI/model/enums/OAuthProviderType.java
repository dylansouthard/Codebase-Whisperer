package info.dylansouthard.StraysBookAPI.model.enums;

public enum OAuthProviderType {
    GOOGLE,
    FACEBOOK,
    LINE;

    public String key() {
        return this.name().toLowerCase();
    }

    public static OAuthProviderType getProvider(String provider) {
        return switch (provider.toUpperCase()) {
            case "GOOGLE" -> GOOGLE;
            case "FACEBOOK" -> FACEBOOK;
            case "LINE" -> LINE;
            default -> null;
        };
    }
}
