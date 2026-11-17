package dpx;

import java.lang.reflect.RecordComponent;
import java.util.*;

/** Very small JSON helper: reads a flat JSON object and writes records/lists as JSON. */
public class Json {

    /** Reads a flat JSON object like {"a":"x","b":2,"c":true} into a Map. */
    public static Map<String, Object> parse(String s) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (s == null || s.isBlank()) return map;
        int[] p = {0};
        skip(s, p);
        expect(s, p, '{');
        skip(s, p);
        if (s.charAt(p[0]) == '}') return map;
        while (true) {
            skip(s, p);
            String key = readString(s, p);
            skip(s, p);
            expect(s, p, ':');
            skip(s, p);
            map.put(key, readValue(s, p));
            skip(s, p);
            char c = s.charAt(p[0]++);
            if (c == '}') break;
            if (c != ',') throw new IllegalArgumentException("Bad JSON");
        }
        return map;
    }

    private static Object readValue(String s, int[] p) {
        char c = s.charAt(p[0]);
        if (c == '"') return readString(s, p);
        int start = p[0];
        while (p[0] < s.length() && ",}".indexOf(s.charAt(p[0])) < 0) p[0]++;
        String t = s.substring(start, p[0]).trim();
        if (t.equals("true")) return Boolean.TRUE;
        if (t.equals("false")) return Boolean.FALSE;
        if (t.equals("null")) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad JSON value: " + t);
        }
    }

    private static String readString(String s, int[] p) {
        expect(s, p, '"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(p[0]++);
            if (c == '"') break;
            if (c == '\\') {
                char n = s.charAt(p[0]++);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'u': sb.append((char) Integer.parseInt(s.substring(p[0], p[0] + 4), 16)); p[0] += 4; break;
                    default: sb.append(n);
                }
            } else sb.append(c);
        }
        return sb.toString();
    }

    private static void skip(String s, int[] p) {
        while (p[0] < s.length() && Character.isWhitespace(s.charAt(p[0]))) p[0]++;
    }

    private static void expect(String s, int[] p, char c) {
        if (p[0] >= s.length() || s.charAt(p[0]) != c) throw new IllegalArgumentException("Bad JSON");
        p[0]++;
    }

    // ---------- writing ----------

    public static String toJson(Object o) {
        if (o == null) return "null";
        if (o instanceof String) return quote((String) o);
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof Collection) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object x : (Collection<?>) o) {
                if (!first) sb.append(',');
                sb.append(toJson(x));
                first = false;
            }
            return sb.append(']').toString();
        }
        if (o instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!first) sb.append(',');
                sb.append(quote(String.valueOf(e.getKey()))).append(':').append(toJson(e.getValue()));
                first = false;
            }
            return sb.append('}').toString();
        }
        if (o.getClass().isRecord()) return toJson(toMap(o));
        return quote(o.toString());
    }

    /** Turns a record into a Map (field name -> value). */
    public static Map<String, Object> toMap(Object record) {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            for (RecordComponent rc : record.getClass().getRecordComponents())
                m.put(rc.getName(), rc.getAccessor().invoke(record));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return m;
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c < 32 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return sb.append('"').toString();
    }
}
