package info.dylansouthard.StraysBookAPI.util;

import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;

public class CareEventWeightUtil {

    public static int weightCareEvent(CareEventType type) {
        if (type == null) return 0;  // ✅ Defensive coding

        return switch (type) {
            case VACCINATED, VET -> 3;
            case STERILIZED -> 5;
            case MEDICATED -> 2;
            default -> 1;
        };
    }

    // Optional: prevent instantiation
    private CareEventWeightUtil() {}
}
