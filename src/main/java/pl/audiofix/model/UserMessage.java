package pl.audiofix.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public record UserMessage(String key, List<Object> args) {

    public UserMessage {

        args = Collections.unmodifiableList(new ArrayList<>(args));
    }

    public static UserMessage of(String key, Object... args) {

        return new UserMessage(key, Arrays.asList(args));
    }

    public Object[] argArray() {

        return args.toArray();
    }
}
