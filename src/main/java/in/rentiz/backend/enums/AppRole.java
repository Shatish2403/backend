package in.rentiz.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AppRole {
    ADMIN("admin"),
   USER("user");


    private final String value;

    AppRole(String value) {this.value = value;};

    @JsonValue
    public String getValue(){
        return value;
    }

    @JsonCreator
    public static AppRole fromValue(String text){
        for (AppRole a : AppRole.values()){
            if (a.value.equalsIgnoreCase(text)) return a;
        }
        throw new IllegalArgumentException("Unexpected Value :" + text);
    }
}

