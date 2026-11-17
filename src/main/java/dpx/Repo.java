package dpx;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.*;

/** In-memory storage for one entity type (a record whose first field is "long id"). */
public class Repo<T extends Record> {
    private final Class<T> type;
    private final Map<Long, T> items = new LinkedHashMap<>();
    private long nextId = 1;

    public Repo(Class<T> type) {
        this.type = type;
    }

    public List<T> findAll() {
        return new ArrayList<>(items.values());
    }

    public Optional<T> findById(long id) {
        return Optional.ofNullable(items.get(id));
    }

    public T create(Map<String, Object> data) {
        T item = build(nextId, data);
        items.put(nextId, item);
        nextId++;
        return item;
    }

    /** Changes only the fields that are present in data. */
    public T update(long id, Map<String, Object> data) {
        Map<String, Object> merged = Json.toMap(items.get(id));
        merged.putAll(data);
        T item = build(id, merged);
        items.put(id, item);
        return item;
    }

    public boolean delete(long id) {
        return items.remove(id) != null;
    }

    private T build(long id, Map<String, Object> data) {
        try {
            RecordComponent[] comps = type.getRecordComponents();
            Class<?>[] types = new Class<?>[comps.length];
            Object[] args = new Object[comps.length];
            for (int i = 0; i < comps.length; i++) {
                types[i] = comps[i].getType();
                Object v = comps[i].getName().equals("id") ? (Object) id : data.get(comps[i].getName());
                args[i] = convert(v, types[i]);
            }
            Constructor<T> c = type.getDeclaredConstructor(types);
            return c.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object convert(Object v, Class<?> t) {
        if (t == long.class) return v instanceof Number ? ((Number) v).longValue() : 0L;
        if (t == double.class) return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
        if (t == boolean.class) return v instanceof Boolean ? v : Boolean.FALSE;
        return v == null ? "" : String.valueOf(v);
    }
}
