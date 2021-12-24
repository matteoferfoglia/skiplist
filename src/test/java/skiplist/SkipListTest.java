package skiplist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SkipListTest {

    final static int NUM_OF_ELEMENTS_TO_ADD_IN_TESTS = 100;
    final static Map<Integer, Integer> MAP_OF_NODES_TO_ADD_IN_TESTS =
            IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)
                    .mapToObj(i -> new AbstractMap.SimpleEntry<>(i, i + 1/*dont care value*/))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    final static int[] EXPECTED_SORTED_DISTINCT_KEYS = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).toArray();
    SkipList<Integer, Integer> skipList;

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
        skipList = new SkipList<>();
        assertEquals(0, skipList.size());
        assertTrue(skipList.isEmpty());
    }

    @Test
    void levelOfTheListIs0WhenJustCreated() {
        final int EXPECTED_LEVEL = 0;
        assertEquals(EXPECTED_LEVEL, new SkipList<>().getListLevel());
    }

    @ParameterizedTest
    @CsvSource({"1,2"})
    void put(int key, int value) {
        skipList.put(key, value);
        final int NUM_OF_ADDED_ELEMENTS = 1;
        assertEquals(NUM_OF_ADDED_ELEMENTS, skipList.size());
        assertFalse(skipList.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"1,2"})
    void putOneElementAndGet(int key, int value) {
        put(key, value);
        assertEquals(value, skipList.get(key));
    }

    @ParameterizedTest
    @CsvSource({"1,2"})
    void putOneElementAndRemove(int key, int value) {
        put(key, value);
        final int CURRENT_SKIP_LIST_SIZE = skipList.size();
        assertEquals(value, skipList.remove(key));
        assertEquals(CURRENT_SKIP_LIST_SIZE - 1, skipList.size());
    }

    @ParameterizedTest
    @CsvSource({"1,2,3"})
    void getReturnsNullIfKeyNotPresent(int key, int value, int anotherKeyNotPresent) {
        skipList.put(key, value);
        assertNull(skipList.get(anotherKeyNotPresent));
    }

    @ParameterizedTest
    @CsvSource({"1,2"})
    void putReturnsNullIfKeyWasNotAlreadyPresent(int key, int value) {
        assert !skipList.containsKey(key);
        assertNull(skipList.put(key, value));
    }

    @ParameterizedTest
    @CsvSource({"1,2,3"})
    void putReturnsTheOldValueIfKeyWasAlreadyPresent(int key, int oldValue, int newValue) {
        skipList.put(key, oldValue);
        assert skipList.containsKey(key);
        assertEquals(oldValue, skipList.put(key, newValue));
        assertEquals(newValue, skipList.get(key));
    }

    @Test
    void elementsAreInsertedInCorrectOrder() {
        final int[] UNORDERED_KEYS_TO_ADD = {9, -5, 0, 1, 2, 7, 5, 4, 6, 2, -1, -2, -3, 10, 100, -100, 0, 0, 0, 0, 1, 2, 3};
        Arrays.stream(UNORDERED_KEYS_TO_ADD).forEach(key -> skipList.put(key, null/*dont care value*/));
        final int[] EXPECTED_ORDERED_DISTINCT_KEYS = Arrays.stream(UNORDERED_KEYS_TO_ADD).distinct().sorted().toArray();
        assert skipList.size() == EXPECTED_ORDERED_DISTINCT_KEYS.length;

        int i = 0;
        for (var key : skipList) {
            assertEquals(EXPECTED_ORDERED_DISTINCT_KEYS[i++], key);
        }
    }

    @Test
    void elementsAreInsertedWithoutDuplicates() {
        final int[] UNORDERED_KEYS_TO_ADD = {9, -5, 0, 1, 2, 7, 5, 4, 6, 2, -1, -2, -3, 10, 100, -100, 0, 0, 0, 0, 1, 2, 3};
        final int NUM_OF_DISTINCT_ELEMENTS = (int) Arrays.stream(UNORDERED_KEYS_TO_ADD).distinct().count();

        Arrays.stream(UNORDERED_KEYS_TO_ADD).forEach(key -> skipList.put(key, null));
        assertEquals(NUM_OF_DISTINCT_ELEMENTS, skipList.size());
        Arrays.stream(UNORDERED_KEYS_TO_ADD).forEach(key -> assertTrue(skipList.containsKey(key)));
    }

    @Test
    void clear() {
        skipList.put(1, 2);
        skipList.put(3, 4);
        skipList.put(5, 6);

        //noinspection ConstantConditions   // precondition for the test
        assert skipList.size() > 0;

        skipList.clear();

        assertEquals(0, skipList.size());
        assertTrue(skipList.isEmpty());
        assertEquals(0, skipList.getListLevel());
    }

    @Test
    void putAll() {
        assert skipList.isEmpty();  // precondition
        skipList.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        assertEquals(NUM_OF_ELEMENTS_TO_ADD_IN_TESTS, MAP_OF_NODES_TO_ADD_IN_TESTS.size());
        MAP_OF_NODES_TO_ADD_IN_TESTS.forEach((key, value) -> {
            assertTrue(skipList.containsKey(key));
            assertTrue(skipList.containsValue(value));
        });
    }

    @ParameterizedTest
    @MethodSource("pairOfIntsWithSecondElementsGreaterOrEqualThanFirst")
    void subMap(int fromKeyIncluded, int toKeyExcluded) {
        skipList.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        var subMap = skipList.subMap(fromKeyIncluded, toKeyExcluded);

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
        skipList.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        var subMap = skipList.headMap(toKeyExcluded);

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
        skipList.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        var subMap = skipList.tailMap(fromKeyIncluded);

        int i = fromKeyIncluded;
        for (var key : subMap.keySet()/*SortedMap iterator keeps ascending order*/) {
            assertEquals(EXPECTED_SORTED_DISTINCT_KEYS[i++], key);
        }
        final int EXPECTED_SUB_MAP_SIZE = skipList.size() - fromKeyIncluded;
        assertEquals(EXPECTED_SUB_MAP_SIZE, subMap.size());
        assertEquals(skipList.size(), i);
    }

    @Test
    void containsValue() {
        final int[] VALUES_TO_ADD = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).map(i -> (int) (Math.random() * NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)).toArray();
        final int[] CORRESPONDING_KEYS = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).toArray();
        Arrays.stream(CORRESPONDING_KEYS)
                .forEach(key -> skipList.put(key, VALUES_TO_ADD[key]));

        // assert list contains all elements that should contain
        for (var key : CORRESPONDING_KEYS) {
            var value = VALUES_TO_ADD[key];
            assertTrue(skipList.containsValue(value));
        }

        // assert list DOES NOT contain any element that should not contain
        IntStream.iterate(-NUM_OF_ELEMENTS_TO_ADD_IN_TESTS / 2, i -> i + 1)
                .filter(valueNotContained -> Arrays.stream(VALUES_TO_ADD).noneMatch(value -> value == valueNotContained))
                .limit(2 * NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)
                .forEach(valueNotContained -> assertFalse(skipList.containsValue(valueNotContained)));
    }

    @Test
    void keySetContainsAllKeysOrdered() {
        var keys = MAP_OF_NODES_TO_ADD_IN_TESTS.keySet().stream().sorted().toList();
        skipList.putAll(MAP_OF_NODES_TO_ADD_IN_TESTS);
        assert skipList.size() == keys.size();
        int i = 0;
        for (var key : skipList.keySet()) {
            assertEquals(keys.get(i++), key);
        }
    }

    @Test
    void valueSetContainsAllValuesOrdered() {
        final int[] SORTED_VALUES = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).map(i -> (int) (Math.random() * NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)).toArray();
        final int[] CORRESPONDING_KEYS = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).toArray();
        Arrays.stream(CORRESPONDING_KEYS)
                .forEach(key -> skipList.put(key, SORTED_VALUES[key]));
        assert skipList.size() == SORTED_VALUES.length;
        int i = 0;
        for (var value : skipList.values()) {
            assertEquals(SORTED_VALUES[i++], value);
        }
    }

    @Test
    void entrySetContainsAllEntriesOrdered() {
        final int[] SORTED_VALUES = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).map(i -> (int) (Math.random() * NUM_OF_ELEMENTS_TO_ADD_IN_TESTS)).toArray();
        final int[] CORRESPONDING_KEYS = IntStream.range(0, NUM_OF_ELEMENTS_TO_ADD_IN_TESTS).toArray();
        Arrays.stream(CORRESPONDING_KEYS)
                .forEach(key -> skipList.put(key, SORTED_VALUES[key]));
        assert skipList.size() == CORRESPONDING_KEYS.length;
        int i = 0;
        for (var entry : skipList.entrySet()) {
            assertEquals(CORRESPONDING_KEYS[i], entry.getKey());
            assertEquals(SORTED_VALUES[i], entry.getValue());
            i++;
        }
    }

}