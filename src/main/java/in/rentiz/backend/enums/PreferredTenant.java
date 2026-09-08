package in.rentiz.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PreferredTenant {
    FAMILY("Family"),
    COUPLE_FRIENDLY("Couple Friendly"),
    BACHELOR_MALE("Bachelor Male"),
    BACHELOR_FEMALE("Bachelor Female"),
    COMPANY("Company");

    private final String value;

    PreferredTenant(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PreferredTenant fromValue(String text) {
        for (PreferredTenant p : PreferredTenant.values()) {
            if (p.value.equalsIgnoreCase(text)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unexpected value for PreferredTenant: " + text);
    }
}