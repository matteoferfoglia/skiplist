package skiplist;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SkipListTest {

    private final static String LIST_ELEMENTS_SEPARATOR = "_";
    private final static String RESOURCE_FILE_PATH = "skipListOperation.csv";

    private static SkipList<Integer> createSkipListOfIntegersFromString(String listAsString) {
        var l = new SkipList<Integer>();
        l.addAll(createListOfIntegersFromString(listAsString));
        return l;
    }

    private static List<Integer> createListOfIntegersFromString(String listAsString) {
        return Arrays.stream(listAsString == null ? new String[0] : listAsString.split(LIST_ELEMENTS_SEPARATOR))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    @ParameterizedTest
    @CsvFileSource(resources = RESOURCE_FILE_PATH, numLinesToSkip = 1)
    void unionWith2InputLists(String l1AsStr, String l2AsStr, String unionAsStr) {
        var l1 = createSkipListOfIntegersFromString(l1AsStr);
        var l2 = createSkipListOfIntegersFromString(l2AsStr);
        var expected = createSkipListOfIntegersFromString(unionAsStr);
        var actual = SkipList.union(l1, l2);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvFileSource(resources = RESOURCE_FILE_PATH, numLinesToSkip = 1)
    void intersectionWith2InputLists(String l1AsStr, String l2AsStr, String ignored, String intersectionAsStr) {
        var l1 = createSkipListOfIntegersFromString(l1AsStr);
        var l2 = createSkipListOfIntegersFromString(l2AsStr);
        var expected = createSkipListOfIntegersFromString(intersectionAsStr);
        var actual = SkipList.intersection(l1, l2);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource({
            ",,,",
            "0,0,0,0",
            "0_1_2_3,-1_0_2_5,-2_-1_0_2_3, 0_2"
    })
    void intersectionVarargs(String l1AsStr, String l2AsStr, String l3AsStr, String expectedIntersectionAsStr) {
        var l1 = createSkipListOfIntegersFromString(l1AsStr);
        var l2 = createSkipListOfIntegersFromString(l2AsStr);
        var l3 = createSkipListOfIntegersFromString(l3AsStr);
        var expected = createSkipListOfIntegersFromString(expectedIntersectionAsStr);
        var actual = SkipList.intersection(l1, l2, l3);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource({
            ",,0,0,",
            "0,0,0,0,0",
            "0_1_2_3,-1_0_2_5,0,2,0_2",
            "0_1_2_3_4,-1_0_2_4_5,0,2,0_2"
    })
    void intersectionWithPredicate(String l1AsStr, String l2AsStr,
                                   int minValueIncluded, int maxValueIncluded,  // used to test a predicate
                                   String expectedIntersectionAsStr) {
        var l1 = createSkipListOfIntegersFromString(l1AsStr);
        var l2 = createSkipListOfIntegersFromString(l2AsStr);
        BiPredicate<Integer, Integer> biPredicate = (a, b) -> minValueIncluded <= a && a <= maxValueIncluded;
        var expected = createSkipListOfIntegersFromString(expectedIntersectionAsStr);
        var actual = SkipList.intersection(l1, l2, biPredicate);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource({
            ",,,",
            "0,0,0,0",
            "0_1_2_3,-1_0_2_5,-2_-1_0_2_3, -2_-1_0_1_2_3_5"
    })
    void unionVarargs(String l1AsStr, String l2AsStr, String l3AsStr, String expectedUnionAsStr) {
        var l1 = createSkipListOfIntegersFromString(l1AsStr);
        var l2 = createSkipListOfIntegersFromString(l2AsStr);
        var l3 = createSkipListOfIntegersFromString(l3AsStr);
        var expected = createSkipListOfIntegersFromString(expectedUnionAsStr);
        var actual = SkipList.union(l1, l2, l3);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvFileSource(resources = RESOURCE_FILE_PATH, numLinesToSkip = 1)
    void merge(String l1AsStr, String l2AsStr, String ignored, String ignored2, String mergeAsStr) {
        var l1 = createSkipListOfIntegersFromString(l1AsStr);
        var l2 = createSkipListOfIntegersFromString(l2AsStr);
        var expected = createSkipListOfIntegersFromString(mergeAsStr);
        assertEquals(expected, l1.merge(l2));
    }

    @ParameterizedTest
    @CsvFileSource(resources = RESOURCE_FILE_PATH, numLinesToSkip = 1)
    void addAll(String l1AsStr, String l2AsStr, String unionAsStr) {
        var l1 = createSkipListOfIntegersFromString(l1AsStr);
        var expected = createSkipListOfIntegersFromString(unionAsStr);
        l1.addAll(createListOfIntegersFromString(l2AsStr));
        assertEquals(expected, l1);
    }

    @ParameterizedTest
    @CsvFileSource(resources = RESOURCE_FILE_PATH, numLinesToSkip = 1)
    void containsAll(String l1AsStr, String l2AsStr, String unionAsStr) {
        var l1 = createSkipListOfIntegersFromString(l1AsStr);
        var l2 = createSkipListOfIntegersFromString(l2AsStr);
        var union = createSkipListOfIntegersFromString(unionAsStr);
        assertTrue(union.containsAll(l1));
        assertTrue(union.containsAll(l2));
    }

    @ParameterizedTest
    @CsvFileSource(resources = RESOURCE_FILE_PATH, numLinesToSkip = 1)
    void toArray(String listAsString) {
        var list = createSkipListOfIntegersFromString(listAsString);
        Object[] toArray = list.toArray();
        assertEquals(list.size(), toArray.length);
        var iterator = list.iterator();
        int i;
        for (i = 0; iterator.hasNext(); i++) {
            assertEquals(iterator.next(), toArray[i]);
        }
        assertEquals(i, toArray.length);
    }

    @ParameterizedTest
    @CsvFileSource(resources = RESOURCE_FILE_PATH, numLinesToSkip = 1)
    void toArray2(String listAsString) {
        List<Integer> integers = createListOfIntegersFromString(listAsString)
                .stream().sorted().collect(Collectors.toList());
        var list = createSkipListOfIntegersFromString(listAsString);
        final int PREFIXED_SIZE_ARRAY = 2;
        Integer[] destArray = new Integer[PREFIXED_SIZE_ARRAY];
        Integer[] toArray = list.toArray(destArray);

        assertEquals(list.size(), toArray.length);
        var iterator = list.iterator();
        int i;
        for (i = 0; iterator.hasNext(); i++) {
            assertEquals(iterator.next(), toArray[i]);
        }
        assertEquals(i, toArray.length);

        var EXPECTED_NUM_OF_ELEMENTS = Math.min(integers.size(), PREFIXED_SIZE_ARRAY);
        assertEquals(integers.subList(0, EXPECTED_NUM_OF_ELEMENTS), Arrays.asList(destArray).subList(0, EXPECTED_NUM_OF_ELEMENTS));

        for (int j = EXPECTED_NUM_OF_ELEMENTS; j < destArray.length; j++) {
            assertNull(destArray[j]);
        }
    }
}