package skiplist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SkipListHashMapTest {

    private final SkipListHashMap<Integer, String> map = new SkipListHashMap<>();

    @BeforeEach
    void clearMap() {
        map.clear();
        assertIsEmpty();
    }

    @Test
    void containsKey() {
        int aKey = 9;
        put(aKey, "");
        assertEquals(1, map.size());
        assertTrue(map.containsKey(aKey));
        assertEquals(1, map.getCopyOfHashMap().size());
        assertTrue(map.getCopyOfHashMap().containsKey(aKey));
    }

    /**
     * Assert that the instance is empty.
     */
    private void assertIsEmpty() {
        assert map.isEmpty();
        assert map.getCopyOfHashMap().isEmpty();
    }

    @Test
    void get() {
        int aKey = 9;
        String aValue = "foo";
        put(aKey, aValue);
        assertEquals(aValue, map.get(aKey));
        assertEquals(aValue, map.getCopyOfHashMap().get(aKey));
    }

    @ParameterizedTest
    @CsvSource({"1, foo", "2, bar"})
    void put(int key, String value) {
        map.put(key, value);
        assertEquals(value, map.get(key));
        assertEquals(value, map.getCopyOfHashMap().get(key));
    }

    @Test
    void remove() {
        int aKey = 9;
        String aValue = "foo";
        put(aKey, aValue);
        map.remove(aKey);
        assertFalse(map.containsKey(aKey));
        assertFalse(map.getCopyOfHashMap().containsKey(aKey));
    }

    @Test
    void copyNodeAndInsertAtEnd() {
        SkipListNode<Integer, String> node = new SkipListNode<>(9, "foo", 1, Comparator.naturalOrder());
        map.copyNodeAndInsertAtEnd(node);
        assertTrue(map.containsKey(node.getKey())); // insertion at end is responsibility of SkipListMap (not to be tested here)
        assertTrue(map.getCopyOfHashMap().containsKey(node.getKey()));
    }

    @Test
    void clear() {
        put(1, "foo");
        assert map.size() > 0;
        assert map.getCopyOfHashMap().size() > 0;
        map.clear();
        assertEquals(0, map.size());
        assertEquals(0, map.getCopyOfHashMap().size());
    }

    @Test
    void putAll() {
        var m = new HashMap<Integer, String>() {{
            put(1, "1");
            put(2, "2");
        }};
        map.putAll(m);
        assertEquals(m.size(), map.size());
        assertEquals(m.size(), map.getCopyOfHashMap().size());
        assertTrue(map.entrySet().stream().allMatch(entry -> Objects.equals(m.get(entry.getKey()), entry.getValue())));
        assertTrue(map.getCopyOfHashMap().entrySet().stream().allMatch(entry -> Objects.equals(m.get(entry.getKey()), entry.getValue())));
    }

    @Test
    void putAllKeys() {
        var keys = Arrays.asList(1, 2, 3);
        map.putAllKeys(keys);
        assertEquals(keys.size(), map.size());
        assertTrue(map.keySet().containsAll(keys));
        assertTrue(map.getCopyOfHashMap().keySet().containsAll(keys));
    }

    @Test
    void putAllKeysWithoutCheckingOrderAtTheEnd() {
        // order is responsibility of SkipListMap, so not tested here
        var keys = Arrays.asList(1, 2, 3);
        map.putAllKeysWithoutCheckingOrderAtTheEnd(keys);
        assertEquals(keys.size(), map.size());
        assertTrue(map.keySet().containsAll(keys));
        assertTrue(map.getCopyOfHashMap().keySet().containsAll(keys));
    }

    @Test
    void readWriteExternal() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(baos));

        var m = new HashMap<Integer, String>() {{
            put(1, "1");
            put(2, "2");
        }};

        map.putAll(m);
        assert map.size() == m.size();
        assert map.getCopyOfHashMap().size() == m.size();

        // write object
        assert baos.size() == 0;
        oos.writeObject(map);
        oos.flush();
        assertTrue(baos.size() > 0);

        // read object
        ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new ByteArrayInputStream(baos.toByteArray())));
        @SuppressWarnings("unchecked")
        SkipListHashMap<Integer, String> read = (SkipListHashMap<Integer, String>) ois.readObject();
        assertEquals(map, read);
        assertEquals(map.getCopyOfHashMap(), read.getCopyOfHashMap());
    }

    @Test
    void getCopyOfHashMapDoesntAllowToEditTheInstance() {
        put(1, "foo");
        var copy = map.getCopyOfHashMap();
        int anotherKey = 2;
        String anotherValue = "bar";
        copy.put(anotherKey, anotherValue);
        assertEquals(map.size() + 1, copy.size());
        assertFalse(map.containsKey(anotherKey));
    }

    @Test
    void getCopyOfHashMapDoesntSeeNextUpdatesOnTheInstance() {
        put(1, "foo");
        var copy = map.getCopyOfHashMap();
        int anotherKey = 2;
        String anotherValue = "bar";
        map.put(anotherKey, anotherValue);
        assertEquals(map.size() - 1, copy.size());
        assertFalse(copy.containsKey(anotherKey));
    }

    @Test
    void getCopyOfHashMap() {
        put(1, "foo");
        put(2, "bar");
        var copy = map.getCopyOfHashMap();
        assertEquals(map.size(), copy.size());
        assertTrue(map.entrySet().stream().allMatch(entry -> Objects.equals(copy.get(entry.getKey()), entry.getValue())));
    }
}