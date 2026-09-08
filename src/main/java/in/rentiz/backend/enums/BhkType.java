package in.rentiz.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BhkType {
    ONE_RK("1RK"),
    ONE_BHK("1BHK"),
    TWO_BHK("2BHK"),
    THREE_BHK("3BHK"),
    FOUR_BHK("4BHK"),
    FOUR_PLUS_BHK("4+BHK"),
    PG("PG");

    private final String value;

    BhkType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static BhkType fromValue(String text) {
        for (BhkType b : BhkType.values()) {
            if (b.value.equalsIgnoreCase(text)) return b;
        }
        throw new IllegalArgumentException("Unexpected value: " + text);
    }
}