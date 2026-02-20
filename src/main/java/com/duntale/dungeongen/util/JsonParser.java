package com.duntale.dungeongen.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recursive JSON parser that supports nested objects, arrays, and all
 * JSON primitive types. No external dependencies — hand-rolled for the
 * Hytale server plugin environment.
 *
 * <p>Supported types:</p>
 * <ul>
 *   <li>Objects → {@code Map<String, Object>}</li>
 *   <li>Arrays → {@code List<Object>}</li>
 *   <li>Strings → {@code String}</li>
 *   <li>Numbers → {@code Long} (integers) or {@code Double} (decimals)</li>
 *   <li>Booleans → {@code Boolean}</li>
 *   <li>null → {@code null}</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class JsonParser {

    private final String json;
    private int pos;

    private JsonParser(@Nonnull String json) {
        this.json = json;
        this.pos = 0;
    }

    // ============================================
    // Public API
    // ============================================

    /**
     * Parse a JSON string into a {@code Map<String, Object>} (if the root is an object)
     * or throw if the root is not an object.
     *
     * @param json the JSON string to parse
     * @return parsed map, or {@code null} if parsing fails
     */
    @Nullable
    public static Map<String, Object> parseObject(@Nonnull String json) {
        try {
            JsonParser parser = new JsonParser(json.trim());
            Object result = parser.parseValue();
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) result;
                return map;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse a JSON string into a {@code List<Object>} (if the root is an array)
     * or throw if the root is not an array.
     *
     * @param json the JSON string to parse
     * @return parsed list, or {@code null} if parsing fails
     */
    @Nullable
    public static List<Object> parseArray(@Nonnull String json) {
        try {
            JsonParser parser = new JsonParser(json.trim());
            Object result = parser.parseValue();
            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) result;
                return list;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse any JSON value (object, array, string, number, boolean, null).
     *
     * @param json the JSON string to parse
     * @return the parsed value, or {@code null} on failure
     */
    @Nullable
    public static Object parse(@Nonnull String json) {
        try {
            JsonParser parser = new JsonParser(json.trim());
            return parser.parseValue();
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================
    // Type Conversion Helpers
    // ============================================

    /**
     * Convert a parsed JSON value to {@code double}.
     *
     * @param val the parsed value (Number or String)
     * @return the double value, or 0.0 if conversion fails
     */
    public static double toDouble(@Nullable Object val) {
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
        }
        return 0.0;
    }

    /**
     * Convert a parsed JSON value to {@code int}.
     *
     * @param val the parsed value (Number or String)
     * @return the int value, or 0 if conversion fails
     */
    public static int toInt(@Nullable Object val) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    /**
     * Convert a parsed JSON value to {@code long}.
     *
     * @param val the parsed value (Number or String)
     * @return the long value, or 0 if conversion fails
     */
    public static long toLong(@Nullable Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
        }
        return 0L;
    }

    /**
     * Convert a parsed JSON value to {@code boolean}.
     *
     * @param val the parsed value (Boolean or String)
     * @return the boolean value, or false if conversion fails
     */
    public static boolean toBoolean(@Nullable Object val) {
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }

    /**
     * Convert a parsed JSON value to {@code String}.
     *
     * @param val the parsed value
     * @return the string value, or {@code null} if the value is null
     */
    @Nullable
    public static String toStringOrNull(@Nullable Object val) {
        if (val == null) return null;
        if (val instanceof String s) return s;
        return val.toString();
    }

    /**
     * Extract a nested {@code Map<String, Object>} from a parent map.
     *
     * @param map the parent map
     * @param key the key to look up
     * @return the nested map, or {@code null} if absent or wrong type
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getObject(@Nonnull Map<String, Object> map, @Nonnull String key) {
        Object val = map.get(key);
        if (val instanceof Map) return (Map<String, Object>) val;
        return null;
    }

    // ============================================
    // Recursive Descent Parser
    // ============================================

    private Object parseValue() {
        skipWhitespace();
        if (pos >= json.length()) throw new IllegalStateException("Unexpected end of JSON");

        char c = json.charAt(pos);
        return switch (c) {
            case '{' -> parseObjectValue();
            case '[' -> parseArrayValue();
            case '"' -> parseString();
            case 't', 'f' -> parseBooleanValue();
            case 'n' -> parseNull();
            default -> {
                if (c == '-' || Character.isDigit(c)) yield parseNumber();
                throw new IllegalStateException("Unexpected character: " + c + " at position " + pos);
            }
        };
    }

    private Map<String, Object> parseObjectValue() {
        Map<String, Object> map = new HashMap<>();
        pos++; // skip '{'
        skipWhitespace();

        if (pos < json.length() && json.charAt(pos) == '}') {
            pos++;
            return map;
        }

        while (pos < json.length()) {
            skipWhitespace();
            // Parse key
            if (json.charAt(pos) != '"') throw new IllegalStateException("Expected '\"' at position " + pos);
            String key = parseString();

            // Skip colon
            skipWhitespace();
            expect(':');
            skipWhitespace();

            // Parse value (recursive)
            Object value = parseValue();
            map.put(key, value);

            // Skip comma or end
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ',') {
                pos++;
            } else if (pos < json.length() && json.charAt(pos) == '}') {
                pos++;
                return map;
            } else {
                throw new IllegalStateException("Expected ',' or '}' at position " + pos);
            }
        }
        throw new IllegalStateException("Unterminated object");
    }

    private List<Object> parseArrayValue() {
        List<Object> list = new ArrayList<>();
        pos++; // skip '['
        skipWhitespace();

        if (pos < json.length() && json.charAt(pos) == ']') {
            pos++;
            return list;
        }

        while (pos < json.length()) {
            skipWhitespace();
            Object value = parseValue();
            list.add(value);

            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ',') {
                pos++;
            } else if (pos < json.length() && json.charAt(pos) == ']') {
                pos++;
                return list;
            } else {
                throw new IllegalStateException("Expected ',' or ']' at position " + pos);
            }
        }
        throw new IllegalStateException("Unterminated array");
    }

    private String parseString() {
        pos++; // skip opening '"'
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == '\\') {
                pos++;
                if (pos >= json.length()) throw new IllegalStateException("Unterminated escape");
                char escaped = json.charAt(pos);
                switch (escaped) {
                    case '"', '\\', '/' -> sb.append(escaped);
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (pos + 4 >= json.length()) throw new IllegalStateException("Incomplete unicode escape");
                        String hex = json.substring(pos + 1, pos + 5);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> sb.append(escaped);
                }
            } else if (c == '"') {
                pos++; // skip closing '"'
                return sb.toString();
            } else {
                sb.append(c);
            }
            pos++;
        }
        throw new IllegalStateException("Unterminated string");
    }

    private Number parseNumber() {
        int start = pos;
        boolean isFloat = false;

        if (json.charAt(pos) == '-') pos++;

        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E') {
                isFloat = true;
                pos++;
                if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) pos++;
            } else {
                break;
            }
        }

        String numStr = json.substring(start, pos);
        if (isFloat) {
            return Double.parseDouble(numStr);
        } else {
            long val = Long.parseLong(numStr);
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                return (long) val; // keep as Long for consistency
            }
            return val;
        }
    }

    private Boolean parseBooleanValue() {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        } else if (json.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new IllegalStateException("Expected boolean at position " + pos);
    }

    private Object parseNull() {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new IllegalStateException("Expected null at position " + pos);
    }

    private void skipWhitespace() {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
    }

    private void expect(char c) {
        if (pos >= json.length() || json.charAt(pos) != c) {
            throw new IllegalStateException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }
}
