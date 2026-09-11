package task3;

import com.google.gson.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Task3 {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Должно быть минимум 3 аргумента: <values.json> <tests.json> <report.json> (<gson|custom]>)");
            return;
        }
        if (args.length >= 4) System.setProperty("json.parser", args[3]);

        JsonParser parser = JsonParsers.create();
        System.out.println("Парсер: " + parser.name());

        JsonObject valuesRoot = parser.parse(Files.readString(Paths.get(args[0]))).asObject();
        Map<Integer, String> idToValue = new HashMap<>();
        for (JsonValue el : valuesRoot.get("values").asArray()) {
            JsonObject o = el.asObject();
            idToValue.put(o.get("id").asInt(), o.get("value").asString());
        }

        JsonObject testsRoot = parser.parse(Files.readString(Paths.get(args[1]))).asObject();
        fillValues(testsRoot.get("tests").asArray(), idToValue);

        Files.writeString(Paths.get(args[2]), parser.write(testsRoot, true));
        System.out.println("Отчёт записан в " + args[2]);
    }

    private static void fillValues(JsonArray tests, Map<Integer, String> idToValue) {
        for (JsonValue el : tests) {
            JsonObject t = el.asObject();
            int id = t.get("id").asInt();
            String value = idToValue.get(id);
            if (value != null) t.putString("value", value);

            JsonValue nested = t.get("values");
            if (nested != null && nested.isArray()) {
                fillValues(nested.asArray(), idToValue);
            }
        }
    }
}

// ======================= JSON DOM =======================

class JsonValue {
    public boolean isArray()  { return this instanceof JsonArray; }

    public JsonObject asObject() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " не объект");
    }
    public JsonArray asArray() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " не массив");
    }
    public String asString() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " не строка");
    }
    public int asInt() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " не число");
    }
}

class JsonObject extends JsonValue {
    private final Map<String, JsonValue> map = new LinkedHashMap<>();

    public void put(String key, JsonValue value) { map.put(key, value); }
    public void putString(String key, String value) { map.put(key, new JsonPrimitive(value)); }

    public JsonValue get(String key) { return map.get(key); }
    public boolean isEmpty() { return map.isEmpty(); }

    public Set<Map.Entry<String, JsonValue>> entrySet() { return map.entrySet(); }

    @Override public JsonObject asObject() { return this; }
}

class JsonArray extends JsonValue implements Iterable<JsonValue> {
    private final List<JsonValue> list = new ArrayList<>();

    public void add(JsonValue v) { list.add(v); }
    public boolean isEmpty()     { return list.isEmpty(); }

    @Override public Iterator<JsonValue> iterator() { return list.iterator(); }
    @Override public JsonArray asArray() { return this; }
}

class JsonPrimitive extends JsonValue {
    public static final JsonPrimitive NULL = new JsonPrimitive();

    private final Object value; // String | BigDecimal | Boolean | null

    private JsonPrimitive() { this.value = null; }
    public JsonPrimitive(String s)     { this.value = s; }
    public JsonPrimitive(BigDecimal n) { this.value = n; }
    public JsonPrimitive(Boolean b)    { this.value = b; }

    public boolean isNull()    { return value == null; }
    public boolean isString()  { return value instanceof String; }
    public boolean isBoolean() { return value instanceof Boolean; }

    @Override public String asString() {
        if (value instanceof String) return (String) value;
        throw new UnsupportedOperationException("Не строка: " + value);
    }
    @Override public int asInt() {
        if (value instanceof BigDecimal) return ((BigDecimal) value).intValueExact();
        if (value instanceof Number)     return ((Number) value).intValue();
        throw new UnsupportedOperationException("Не число: " + value);
    }
    public BigDecimal asNumber() {
        if (value instanceof BigDecimal) return (BigDecimal) value;
        throw new UnsupportedOperationException("Не число: " + value);
    }
    public boolean asBoolean() {
        if (value instanceof Boolean) return (Boolean) value;
        throw new UnsupportedOperationException("Не boolean: " + value);
    }

    public String rawText() {
        if (value == null) return "null";
        if (value instanceof BigDecimal) return ((BigDecimal) value).toPlainString();
        return value.toString();
    }
}

// ======================= Парсеры =======================

interface JsonParser {
    JsonValue parse(String text);
    String write(JsonValue value, boolean pretty);
    String name();
}

class SimpleJsonParser implements JsonParser {

    private String s;
    private int pos;

    @Override public String name() { return "simple"; }

    @Override
    public JsonValue parse(String text) {
        this.s = text;
        this.pos = 0;
        skipWs();
        JsonValue v = parseValue();
        skipWs();
        if (pos < s.length())
            throw new IllegalArgumentException("Лишние символы в позиции " + pos);
        return v;
    }

    private JsonValue parseValue() {
        skipWs();
        char c = peek();
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> new JsonPrimitive(parseString());
            case 't' -> {
                expect("true");
                yield new JsonPrimitive(Boolean.TRUE);
            }
            case 'f' -> {
                expect("false");
                yield new JsonPrimitive(Boolean.FALSE);
            }
            case 'n' -> {
                expect("null");
                yield JsonPrimitive.NULL;
            }
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) yield parseNumber();
                throw new IllegalArgumentException("Неожиданный символ '" + c + "' в позиции " + pos);
            }
        };
    }

    private JsonObject parseObject() {
        JsonObject obj = new JsonObject();
        pos++;
        skipWs();
        if (peek() == '}') { pos++; return obj; }
        while (true) {
            skipWs();
            String key = parseString();
            skipWs();
            if (peek() != ':') throw new IllegalArgumentException("Ожидалось ':' в позиции " + pos);
            pos++;
            JsonValue v = parseValue();
            obj.put(key, v);
            skipWs();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == '}') { pos++; break; }
            throw new IllegalArgumentException("Ожидалось ',' или '}' в позиции " + pos);
        }
        return obj;
    }

    private JsonArray parseArray() {
        JsonArray arr = new JsonArray();
        pos++;
        skipWs();
        if (peek() == ']') { pos++; return arr; }
        while (true) {
            arr.add(parseValue());
            skipWs();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == ']') { pos++; break; }
            throw new IllegalArgumentException("Ожидалось ',' или ']' в позиции " + pos);
        }
        return arr;
    }

    private String parseString() {
        if (peek() != '"') throw new IllegalArgumentException("Ожидалось '\"' в позиции " + pos);
        pos++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= s.length()) throw new IllegalArgumentException("Незакрытая строка");
            char c = s.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = s.charAt(pos++);
                switch (e) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default: throw new IllegalArgumentException("Плохой escape \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private JsonValue parseNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
        if (pos < s.length() && s.charAt(pos) == '.') {
            pos++;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
        }
        if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
            pos++;
            if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
        }
        return new JsonPrimitive(new BigDecimal(s.substring(start, pos)));
    }

    private void expect(String word) {
        if (!s.startsWith(word, pos))
            throw new IllegalArgumentException("Ожидалось '" + word + "' в позиции " + pos);
        pos += word.length();
    }
    private char peek() {
        if (pos >= s.length()) throw new IllegalArgumentException("Неожиданный конец ввода");
        return s.charAt(pos);
    }
    private void skipWs() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
    }

    @Override
    public String write(JsonValue value, boolean pretty) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, pretty, 0);
        return sb.toString();
    }

    private void writeValue(StringBuilder sb, JsonValue v, boolean pretty, int indent) {
        if (v instanceof JsonObject o) {
            if (o.isEmpty()) { sb.append("{}"); return; }
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, JsonValue> e : o.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                if (pretty) { sb.append('\n'); indent(sb, indent + 1); }
                writeString(sb, e.getKey());
                sb.append(pretty ? ": " : ":");
                writeValue(sb, e.getValue(), pretty, indent + 1);
            }
            if (pretty) { sb.append('\n'); indent(sb, indent); }
            sb.append('}');
        } else if (v instanceof JsonArray a) {
            if (a.isEmpty()) { sb.append("[]"); return; }
            sb.append('[');
            boolean first = true;
            for (JsonValue e : a) {
                if (!first) sb.append(',');
                first = false;
                if (pretty) { sb.append('\n'); indent(sb, indent + 1); }
                writeValue(sb, e, pretty, indent + 1);
            }
            if (pretty) { sb.append('\n'); indent(sb, indent); }
            sb.append(']');
        } else {
            JsonPrimitive p = (JsonPrimitive) v;
            if (p.isNull()) sb.append("null");
            else if (p.isString()) writeString(sb, p.asString());
            else sb.append(p.rawText());
        }
    }

    private void indent(StringBuilder sb, int n) {
        sb.append("  ".repeat(Math.max(0, n)));
    }

    private void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }
}

class GsonJsonParser implements JsonParser {

    private final Gson pretty = new GsonBuilder().setPrettyPrinting().create();
    private final Gson compact = new Gson();

    @Override public String name() { return "gson"; }

    @Override
    public JsonValue parse(String text) {
        return convert(com.google.gson.JsonParser.parseString(text));
    }

    private JsonValue convert(JsonElement el) {
        if (el.isJsonObject()) {
            JsonObject o = new JsonObject();
            for (Map.Entry<String, JsonElement> e
                    : el.getAsJsonObject().entrySet())
                o.put(e.getKey(), convert(e.getValue()));
            return o;
        }
        if (el.isJsonArray()) {
            JsonArray a = new JsonArray();
            for (JsonElement e : el.getAsJsonArray()) a.add(convert(e));
            return a;
        }
        if (el.isJsonNull()) return JsonPrimitive.NULL;
        com.google.gson.JsonPrimitive p = el.getAsJsonPrimitive();
        if (p.isString())  return new JsonPrimitive(p.getAsString());
        if (p.isBoolean()) return new JsonPrimitive(p.getAsBoolean());
        return new JsonPrimitive(p.getAsBigDecimal());
    }

    @Override
    public String write(JsonValue value, boolean prettyPrint) {
        JsonElement el = toGson(value);
        return (prettyPrint ? pretty : compact).toJson(el);
    }

    private JsonElement toGson(JsonValue v) {
        if (v instanceof JsonObject) {
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            for (Map.Entry<String, JsonValue> e : ((JsonObject) v).entrySet())
                o.add(e.getKey(), toGson(e.getValue()));
            return o;
        }
        if (v instanceof JsonArray) {
            com.google.gson.JsonArray a = new com.google.gson.JsonArray();
            for (JsonValue e : (JsonArray) v) a.add(toGson(e));
            return a;
        }
        JsonPrimitive p = (JsonPrimitive) v;
        if (p.isNull())    return JsonNull.INSTANCE;
        if (p.isString())  return new com.google.gson.JsonPrimitive(p.asString());
        if (p.isBoolean()) return new com.google.gson.JsonPrimitive(p.asBoolean());
        return new com.google.gson.JsonPrimitive(p.asNumber());
    }
}

final class JsonParsers {

    public static JsonParser create() {
        String mode = System.getProperty("json.parser", "auto").toLowerCase();
        switch (mode) {
            case "custom": return new SimpleJsonParser();
            case "gson":   return newGsonOrFail();
            case "auto":
                try { return newGsonOrFail(); }
                catch (Throwable t) { return new SimpleJsonParser(); }
            default:
                throw new IllegalArgumentException("Неизвестный json.parser: " + mode);
        }
    }

    private static JsonParser newGsonOrFail() {
        try {
            Class.forName("com.google.gson.Gson");
            return (JsonParser) Class.forName("task3.GsonJsonParser")
                    .getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            throw new IllegalStateException("Gson недоступен на classpath", t);
        }
    }
}
