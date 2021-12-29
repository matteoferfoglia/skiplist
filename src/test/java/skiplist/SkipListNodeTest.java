package skiplist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SkipListNodeTest {

    private static void methodDidNotThrowButShouldHave() {
        fail("Method did not throw but should have.");
    }

    @Test
    void comparisonOfTwoNullKeysReturnFalse() {
        final int SAMPLE_LEVEL = 1;
        assertFalse(new SkipListNode<>((Integer) null, SAMPLE_LEVEL).isKeyLowerThan(null));
    }

    @Test
    void comparisonOfKeysOfIncompatibleTypesThrows() {
        try {
            final int SAMPLE_LEVEL = 1;
            new SkipListNode<>(10, SAMPLE_LEVEL).isKeyLowerThan("incompatible type");
            methodDidNotThrowButShouldHave();
        } catch (ClassCastException ignored) {
            // should be here
        }
    }

    @ParameterizedTest
    @CsvSource({"1,2,true", "2,1,false", "0,0,false"})
    void isKeyLower(int key1, int key2, boolean keyIsLower) {
        final int SAMPLE_LEVEL = 1;
        assertEquals(keyIsLower, new SkipListNode<>(key1, SAMPLE_LEVEL).isKeyLowerThan(key2));
    }

    @ParameterizedTest
    @CsvSource({"1,2,false", "2,1,false", "0,0,true", "1,1,true", "-1,-1,true", "10,10,true"})
    void isSameKey(int key1, int key2, boolean keyIsLower) {
        final int SAMPLE_LEVEL = 1;
        assertEquals(keyIsLower, new SkipListNode<>(key1, SAMPLE_LEVEL).isSameKey(key2));
    }
}