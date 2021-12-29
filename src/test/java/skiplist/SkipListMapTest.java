package skiplist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SkipListMapTest {

    final static int NUM_OF_ELEMENTS_TO_ADD_IN_TESTS = 100;
    final static Map<Integer, Integer> MAP_OF_NODES_TO_ADD_IN_TESTS =
            IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)
                    .mapToObj(i -> new AbstractMap.SimpleEntry<>(i, i + 1/*dont care value*/))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    final static int[] EXPECTED_SORTED_DISTINCT_KEYS = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).toArray();
    SkipListMap<Integer, Integer> skipListMap;

    public static Stream<Arguments> rangeOfInts() {
        return IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)
                .mapToObj(Arguments::of);
    }

    public static Stream<Arguments> pairOfIntsWithSecondElementsGreaterOrEqualThanFirst() {
        return IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)
                .boxed()
                .flatMap(i -> IntStream.range(i, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)
                        .mapToObj(j -> Arguments.of(i, j)));
    }

    @BeforeEach
    void createEmptySkipList() {
        skipListMap = new SkipListMap<>();
        assertEquals(0, skipListMap.size());
        assertTrue(skipListMap.isEmpty());
    }

    @Test
    void levelOfTheListIs0WhenJustCreated() {
        final int EXPECTED_LEVEL = 0;
        assertEquals(EXPECTED_LEVEL, new SkipListMap<>().getListLevel());
    }

    @ParameterizedTest
    @CsvSource({"1,2"})
    void put(int key, int value) {
        skipListMap.put(key, value);
        final int NUM_OF_ADDED_ELEMENTS = 1;
        assertEquals(NUM_OF_ADDED_ELEMENTS, skipListMap.size());
        assertFalse(skipListMap.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"1,2"})
    void putOneElementAndGet(int key, int value) {
        put(key, value);
        assertEquals(value, skipListMap.get(key));
    }

    @ParameterizedTest
    @CsvSource({"1,2"})
    void putOneElementAndRemove(int key, int value) {
        put(key, value);
        final int CURRENT_SKIP_LIST_SIZE = skipListMap.size();
        assertEquals(value, skipListMap.remove(key));
        assertEquals(CURRENT_SKIP_LIST_SIZE - 1, skipListMap.size());
    }

    @ParameterizedTest
    @CsvSource({"1,2,3"})
    void getReturnsNullIfKeyNotPresent(int key, int value, int anotherKeyNotPresent) {
        skipListMap.put(key, value);
        assertNull(skipListMap.get(anotherKeyNotPresent));
    }

    @ParameterizedTest
    @CsvSource({"1,2"})
    void putReturnsNullIfKeyWasNotAlreadyPresent(int key, int value) {
        assert !skipListMap.containsKey(key);
        assertNull(skipListMap.put(key, value));
    }

    @ParameterizedTest
    @CsvSource({"1,2,3"})
    void putReturnsTheOldValueIfKeyWasAlreadyPresent(int key, int oldValue, int newValue) {
        skipListMap.put(key, oldValue);
        assert skipListMap.containsKey(key);
        assertEquals(oldValue, skipListMap.put(key, newValue));
        assertEquals(newValue, skipListMap.get(key));
    }

    @Test
    void elementsAreInsertedInCorrectOrder() {
        final int[] UNORDERED_KEYS_TO_ADD = {9, -5, 0, 1, 2, 7, 5, 4, 6, 2, -1, -2, -3, 10, 100, -100, 0, 0, 0, 0, 1, 2, 3};
        Arrays.stream(UNORDERED_KEYS_TO_ADD).forEach(key -> skipListMap.put(key, null/*dont care value*/));
        final int[] EXPECTED_ORDERED_DISTINCT_KEYS = Arrays.stream(UNORDERED_KEYS_TO_ADD).distinct().sorted().toArray();
        assert skipListMap.size() == EXPECTED_ORDERED_DISTINCT_KEYS.length;

        int i = 0;
        for (var key : skipListMap) {
            assertEquals(EXPECTED_ORDERED_DISTINCT_KEYS[i++], key);
        }
    }

    @Test
    void elementsAreInsertedWithoutDuplicates() {
        final int[] UNORDERED_KEYS_TO_ADD = {9, -5, 0, 1, 2, 7, 5, 4, 6, 2, -1, -2, -3, 10, 100, -100, 0, 0, 0, 0, 1, 2, 3};
        final int NUM_OF_DISTINCT_ELEMENTS = (int) Arrays.stream(UNORDERED_KEYS_TO_ADD).distinct().count();

        Arrays.stream(UNORDERED_KEYS_TO_ADD).forEach(key -> skipListMap.put(key, null));
        assertEquals(NUM_OF_DISTINCT_ELEMENTS, skipListMap.size());
        Arrays.stream(UNORDERED_KEYS_TO_ADD).forEach(key -> assertTrue(skipListMap.containsKey(key)));
    }

    @Test
    void clear() {
        skipListMap.put(1, 2);
        skipListMap.put(3, 4);
        skipListMap.put(5, 6);

        //noinspection ConstantConditions   // precondition for the test
        assert skipListMap.size() > 0;

        skipListMap.clear();

        assertEquals(0, skipListMap.size());
        assertTrue(skipListMap.isEmpty());
        assertEquals(0, skipListMap.getListLevel());
    }

    @Test
    void putAll() {
        assert skipListMap.isEmpty();  // precondition
        skipListMap.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        assertEquals(NUM_OF_ELEMENTS_TO_ADD_IN_TESTS, MAP_OF_NODES_TO_ADD_IN_TESTS.size());
        MAP_OF_NODES_TO_ADD_IN_TESTS.forEach((key, value) -> {
            var node = new NodeFinder<>(skipListMap.getHeader()).findNextNode(key);
            assertNotNull(node);
            assertEquals(key, node.getKey());
            assertEquals(value, skipListMap.get(key));
        });
    }

    @Test
    void putAllKeys() {
        assert skipListMap.isEmpty();  // precondition
        assertTrue(skipListMap.putAllKeys(MAP_OF_NODES_TO_ADD_IN_TESTS.keySet()));
        assertEquals(NUM_OF_ELEMENTS_TO_ADD_IN_TESTS, MAP_OF_NODES_TO_ADD_IN_TESTS.size());
        MAP_OF_NODES_TO_ADD_IN_TESTS.forEach((key, ignored) -> {
            var node = new NodeFinder<>(skipListMap.getHeader()).findNextNode(key);
            assertNotNull(node);
            assertEquals(key, node.getKey());
            assertNull(skipListMap.get(key));
        });
    }

    @ParameterizedTest
    @MethodSource("pairOfIntsWithSecondElementsGreaterOrEqualThanFirst")
    void subMap(int fromKeyIncluded, int toKeyExcluded) {
        skipListMap.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        var subMap = skipListMap.subMap(fromKeyIncluded, toKeyExcluded);

        int i = fromKeyIncluded;
        for (var key : subMap.keySet()/*SortedMap iterator keeps ascending order*/) {
            assertEquals(EXPECTED_SORTED_DISTINCT_KEYS[i++], key);
        }
        final int EXPECTED_SUB_MAP_SIZE = toKeyExcluded - fromKeyIncluded;
        assertEquals(EXPECTED_SUB_MAP_SIZE, subMap.size());
        assertEquals(toKeyExcluded, i);
    }

    @ParameterizedTest
    @MethodSource("rangeOfInts")
    void headMap(int toKeyExcluded) {
        skipListMap.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        var subMap = skipListMap.headMap(toKeyExcluded);

        int i = 0;
        for (var key : subMap.keySet()/*SortedMap iterator keeps ascending order*/) {
            assertEquals(EXPECTED_SORTED_DISTINCT_KEYS[i++], key);
        }
        assertEquals(toKeyExcluded, subMap.size());
        assertEquals(toKeyExcluded, i);
    }

    @ParameterizedTest
    @MethodSource("rangeOfInts")
    void tailMap(int fromKeyIncluded) {
        skipListMap.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        var subMap = skipListMap.tailMap(fromKeyIncluded);

        int i = fromKeyIncluded;
        for (var key : subMap.keySet()/*SortedMap iterator keeps ascending order*/) {
            assertEquals(EXPECTED_SORTED_DISTINCT_KEYS[i++], key);
        }
        final int EXPECTED_SUB_MAP_SIZE = skipListMap.size() - fromKeyIncluded;
        assertEquals(EXPECTED_SUB_MAP_SIZE, subMap.size());
        assertEquals(skipListMap.size(), i);
    }

    @Test
    void containsValue() {
        final int[] VALUES_TO_ADD = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).map(i -> (int) (Math.random() * NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)).toArray();
        final int[] CORRESPONDING_KEYS = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).toArray();
        Arrays.stream(CORRESPONDING_KEYS)
                .forEach(key -> skipListMap.put(key, VALUES_TO_ADD[key]));

        // assert list contains all elements that should contain
        for (var key : CORRESPONDING_KEYS) {
            var value = VALUES_TO_ADD[key];
            assertTrue(skipListMap.containsValue(value));
        }

        // assert list DOES NOT contain any element that should not contain
        IntStream.iterate(-NUM_OF_ELEMENTS_TO_ADD_IN_TESTS / 2, i -> i + 1)
                .filter(valueNotContained -> Arrays.stream(VALUES_TO_ADD).noneMatch(value -> value == valueNotContained))
                .limit(2 * NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)
                .forEach(valueNotContained -> assertFalse(skipListMap.containsValue(valueNotContained)));
    }

    @Test
    void keySetContainsAllKeysOrdered() {
        var keys = MAP_OF_NODES_TO_ADD_IN_TESTS.keySet().stream().sorted().collect(Collectors.toUnmodifiableList());
        skipListMap.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        assert skipListMap.size() == keys.size();
        int i = 0;
        for (var key : skipListMap.keySet()) {
            assertEquals(keys.get(i++), key);
        }
    }

    @Test
    void valueSetContainsAllValuesOrdered() {
        final int[] SORTED_VALUES = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).map(i -> (int) (Math.random() * NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)).toArray();
        final int[] CORRESPONDING_KEYS = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).toArray();
        Arrays.stream(CORRESPONDING_KEYS)
                .forEach(key -> skipListMap.put(key, SORTED_VALUES[key]));
        assert skipListMap.size() == SORTED_VALUES.length;
        int i = 0;
        for (var value : skipListMap.values()) {
            assertEquals(SORTED_VALUES[i++], value);
        }
    }

    @Test
    void entrySetContainsAllEntriesOrdered() {
        final int[] SORTED_VALUES = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).map(i -> (int) (Math.random() * NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)).toArray();
        final int[] CORRESPONDING_KEYS = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).toArray();
        Arrays.stream(CORRESPONDING_KEYS)
                .forEach(key -> skipListMap.put(key, SORTED_VALUES[key]));
        assert skipListMap.size() == CORRESPONDING_KEYS.length;
        int i = 0;
        for (var entry : skipListMap.entrySet()) {
            assertEquals(CORRESPONDING_KEYS[i], entry.getKey());
            assertEquals(SORTED_VALUES[i], entry.getValue());
            i++;
        }
    }

    @Test
    void readWriteExternal() throws IOException, ClassNotFoundException {
        assert skipListMap.isEmpty();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(baos));

        skipListMap.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        assert skipListMap.size() == MAP_OF_NODES_TO_ADD_IN_TESTS.size();

        // write object
        assert baos.size() == 0;
        oos.writeObject(skipListMap);
        oos.flush();
        assertTrue(baos.size() > 0);

        // read object
        ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new ByteArrayInputStream(baos.toByteArray())));
        @SuppressWarnings("unchecked")
        SkipListMap<Integer, Integer> read = (SkipListMap<Integer, Integer>) ois.readObject();
        assertEquals(skipListMap, read);
    }

    @Test
    void setMaxListLevel() {
        skipListMap.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        assert !skipListMap.isEmpty();
        final int initialMaxListLevel = skipListMap.getMaxListLevel();
        final int newMaxListLevel = initialMaxListLevel + 1;
        var initialEntries = skipListMap.entrySet();
        skipListMap.setMaxListLevel(newMaxListLevel);
        assertEquals(newMaxListLevel, skipListMap.getMaxListLevel());
        initialEntries.stream()
                .unordered().parallel()
                .forEach(entry -> {
                    var key = entry.getKey();
                    assertTrue(skipListMap.containsKey(key));
                    assertEquals(entry.getValue(), skipListMap.get(key));
                });
        assertEquals(initialEntries.size(), skipListMap.size());
    }

    @Test
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    void equalSkipListMapsHaveSameHashCodeWithEmptyInstances() {
        var skipListMap1 = new SkipListMap<>();
        var skipListMap2 = new SkipListMap<>();
        assert skipListMap1.equals(skipListMap2);
        assertEquals(skipListMap1.hashCode(), skipListMap2.hashCode());
    }

    @Test
    void equalSkipListMapsHaveSameHashCodeWithNonEmptyInstances() {
        SkipListMap<Integer, Integer> skipListMap1 = new SkipListMap<>();
        SkipListMap<Integer, Integer> skipListMap2 = new SkipListMap<>();
        skipListMap1.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        skipListMap2.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        assert !skipListMap1.isEmpty();
        assertNotEquals(0, skipListMap1.size());
        assert skipListMap1.equals(skipListMap2);
        assertEquals(skipListMap1.hashCode(), skipListMap2.hashCode());
    }

    @Test
    void hashCodeChangesAfterRemovingNode() {
        SkipListMap<Integer, Integer> skipListMap1 = new SkipListMap<>();
        SkipListMap<Integer, Integer> skipListMap2 = new SkipListMap<>();
        skipListMap1.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        skipListMap2.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        assert !skipListMap1.isEmpty();
        assert skipListMap1.hashCode() == skipListMap2.hashCode();

        var keyPresentForSure = 0;
        assert skipListMap1.containsKey(keyPresentForSure);
        var value = skipListMap1.remove(keyPresentForSure);
        assertNotEquals(skipListMap1.hashCode(), skipListMap2.hashCode());

        skipListMap1.put(keyPresentForSure, value);
        assertEquals(skipListMap1.hashCode(), skipListMap2.hashCode());
    }
}