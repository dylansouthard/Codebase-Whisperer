package info.dylansouthard.StraysBookAPI.model.enums;

public enum PlatformType {
    IOS,
    ANDROID,
    WEB;

    static boolean isMobile(String platform) {
        return switch (platform.toUpperCase()) {
            case "ANDROID", "IOS" -> true;
            case "WEB" -> false;
            default -> true;
        };
    }
}
