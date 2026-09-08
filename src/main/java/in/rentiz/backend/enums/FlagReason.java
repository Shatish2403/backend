package in.rentiz.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FlagReason {
    FAKE_LISTING("fake_listing"),
    WRONG_CONTACT("wrong_contact"),
    ALREADY_RENTED("already_rented"),
    SPAM_OR_SCAM("spam_or_scam"),
    INAPPROPRIATE("inappropriate"),
    OTHER("other");

    private final String value;

    FlagReason(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FlagReason fromValue(String text) {
        for (FlagReason f : FlagReason.values()) {
            if (f.value.equalsIgnoreCase(text)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unexpected value for FlagReason: " + text);
    }
}