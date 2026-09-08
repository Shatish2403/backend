package in.rentiz.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AvailabilityBucket {
    IMMEDIATE("Immediate"),
    WITHIN_15_DAYS("Within 15 Days"),
    WITHIN_30_DAYS("Within 30 Days"),
    AFTER_30_DAYS("After 30 Days");

    private final String value;

    AvailabilityBucket(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static AvailabilityBucket fromValue(String text) {
        for (AvailabilityBucket a : AvailabilityBucket.values()) {
            if (a.value.equalsIgnoreCase(text)) return a;
        }
        throw new IllegalArgumentException("Unexpected value: " + text);
    }
}