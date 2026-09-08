package in.rentiz.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PropertyType {
    APARTMENT("Apartment"),
    INDEPENDENT_HOUSE_VILLA("Independent House/Villa"),
    GATED_COMMUNITY_VILLA("Gated Community Villa"),
    GATED_SOCIETY("Gated Society");

    private final String value;

    PropertyType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static PropertyType fromValue(String text) {
        for (PropertyType p : PropertyType.values()) {
            if (p.value.equalsIgnoreCase(text)) return p;
        }
        throw new IllegalArgumentException("Unexpected value: " + text);
    }
}