package org.nonprofit.jobs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

record InfraiEnvelope(boolean ok, String error) {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern ERROR = Pattern.compile(
            "\\\"error\\\"\\s*:\\s*(null|\\{(?:[^\\\"{}]|\\\"(?:\\\\.|[^\\\"\\\\])*\\\")*\\}|\\\"(?:\\\\.|[^\\\"\\\\])*\\\")");

    static InfraiEnvelope parse(String json) {
        Matcher okMatch = OK.matcher(json);
        if (!okMatch.find()) {
            throw new IllegalArgumentException("Response is not an Infrai envelope");
        }
        Matcher errorMatch = ERROR.matcher(json);
        String error = errorMatch.find() ? errorMatch.group(1) : "null";
        return new InfraiEnvelope(Boolean.parseBoolean(okMatch.group(1)), error);
    }
}
