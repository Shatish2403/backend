package in.rentiz.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ListedBy {
    OWNER("owner"),
    BROKER("broker");

    private final String value;

    ListedBy(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static ListedBy fromValue(String text) {
        for (ListedBy l : ListedBy.values()) {
            if (l.value.equalsIgnoreCase(text)) return l;
        }
        throw new IllegalArgumentException("Unexpected value: " + text);
    }
}