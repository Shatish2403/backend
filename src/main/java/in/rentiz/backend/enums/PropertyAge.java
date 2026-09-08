package in.rentiz.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PropertyAge {
    LESS_THAN_1("<1 year"),
    LESS_THAN_3("<3 years"),
    LESS_THAN_5("<5 years"),
    LESS_THAN_10("<10 years"),
    PLUS_10("10+ years");

    private final String value;

    PropertyAge(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PropertyAge fromValue(String text) {
        for (PropertyAge a : PropertyAge.values()) {
            if (a.value.equalsIgnoreCase(text)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unexpected value for PropertyAge: " + text);
    }
}