package in.rentiz.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AppealStatus {
    NONE("none"),
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String value;

    AppealStatus(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static AppealStatus fromValue(String text) {
        for (AppealStatus a : AppealStatus.values()) {
            if (a.value.equalsIgnoreCase(text)) return a;
        }
        throw new IllegalArgumentException("Unexpected value: " + text);
    }
}