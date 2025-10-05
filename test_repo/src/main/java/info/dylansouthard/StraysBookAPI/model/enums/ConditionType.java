package info.dylansouthard.StraysBookAPI.model.enums;

public enum ConditionType {
    HEALTHY,
    PARASITES,
    ILL,
    INJURED,
    PREGNANT,
    NURSING_OFFSPRING,
    NURSING_SELF,
    UNDERFED,
    ON_MEDICATION;

    public ConditionSeverity getSeverity () {
        return switch (this) {
            case HEALTHY -> ConditionSeverity.POSITIVE;
            case PREGNANT, NURSING_OFFSPRING, NURSING_SELF, ON_MEDICATION -> ConditionSeverity.CAUTION;
            case ILL, INJURED, UNDERFED, PARASITES -> ConditionSeverity.NEGATIVE;
        };
    }

}
