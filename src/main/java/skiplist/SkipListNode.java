package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An instance of this class represents a node of {@link SkipListMap}.
 * Each node has a key and forward pointers to other keys of the {@link SkipListMap}
 * to which this node belongs.
 *
 * @param <K> The type of the key.
 * @author Matteo Ferfoglia.
 */
@SuppressWarnings("UnusedReturnValue")
// returned values of some methods are not used in project, but they can be useful
public class SkipListNode<K extends Comparable<K>> {

    /**
     * The minimum value allowed for the level of the node.
     * Indexing of forward pointers will be from the value of
     * this field minus 1 (included) to {@link #getLevel()} (excluded).
     */
    private static final int MINIMUM_VALID_LEVEL_INCLUDED = 1;

    /**
     * The key of this element.
     */
    @Nullable
    private final K key;
    /**
     * The array of {@link SkipListNode forward pointers}.
     * The size of this a list is the level of this node.
     * A node is said to be a "level i node" if it
     * has i forward pointers, indexed 0 through i-1.
     */
    @Nullable   // inner elements can be null
    private final SkipListNode<K>[] forwardPointers;

    /**
     * Creates a new instance of this class.
     *
     * @param key   The key of this instance.
     * @param level The level of this node.
     */
    public SkipListNode(@Nullable final K key, int level) {
        if (level < MINIMUM_VALID_LEVEL_INCLUDED) {
            throw new IllegalArgumentException("The node level must be at least " + MINIMUM_VALID_LEVEL_INCLUDED);
        }
        this.key = key;
        //noinspection unchecked    // generic array creation
        this.forwardPointers = (SkipListNode<K>[]) new SkipListNode[level];
    }

    /**
     * Copy constructor.
     * <strong>Important</strong>: Forward pointers are <strong>NOT</strong>
     * copied and initialized to <code>null</code>.
     *
     * @param skipListNode The node to be copied.
     * @param nodeLevel    The new level for the node.
     */
    public SkipListNode(@NotNull SkipListNode<K> skipListNode, int nodeLevel) {
        this.key = skipListNode.key;
        //noinspection unchecked    // generic array creation
        this.forwardPointers = (SkipListNode<K>[]) new SkipListNode[nodeLevel];
    }

    @Override
    public String toString() {
        return "{key: " + key + ", forwardsToKeys: " + getForwardPointersKeys() + "}";
    }

    /**
     * @return the {@link List} with the keys of the forwarded nodes by this instance,
     * or with null elements in correspondence of null forward pointers.
     */
    @NotNull
    protected List<K> getForwardPointersKeys() {
        return Arrays.stream(forwardPointers)
                .sequential()
                .map(skipListNode -> skipListNode != null ? skipListNode.getKey() : null)
                .collect(Collectors.toList());
    }

    @Nullable
    public K getKey() {
        return key;
    }

    /**
     * @return The level of this node (the minimum value is 0, if the node has not
     * any forward pointers) and coincides with the number of forward pointers of
     * this instance.
     */
    protected int getLevel() {
        return forwardPointers.length;
    }

    /**
     * Check the validity of the given level before getting
     * or setting the {@link SkipListNode} at the specified
     * level.
     *
     * @param level                         The level (it is an index, 0 is the minimum allowed).
     * @param getter                        true if getter, false if setter.
     * @param newValueIfSetterMustBeInvoked The new value to be set (if
     *                                      setter must be invoked).
     * @return the instance at the specified level if getter was invoked
     * (returned valued can be null if given level is not set), this instance
     * if setter was invoked.
     * @throws IndexOutOfBoundsException If invalid level is given.
     */
    @Nullable
    private SkipListNode<K> checkValidInputLevelBeforeGetOrSet(
            int level, boolean getter, @Nullable SkipListNode<K> newValueIfSetterMustBeInvoked)
            throws IndexOutOfBoundsException {

        if (0 <= level && level < getLevel()) {
            if (getter) {
                return forwardPointers[level];                                      // getter
            } else {
                @Nullable var oldValue = forwardPointers[level];
                forwardPointers[level] = newValueIfSetterMustBeInvoked;             // setter
                return oldValue;
            }
        } else {
            throw new IndexOutOfBoundsException(
                    "Allowed level values for this node must be between " + (MINIMUM_VALID_LEVEL_INCLUDED - 1)
                            + " (included) and " + getLevel() + " (excluded), but was " + level);
        }
    }

    /**
     * @param level The level for which the forward pointer is desired.
     * @return The forward pointer (eventually null if not set) for the given level.
     */
    @Nullable
    public SkipListNode<K> getNext(final int level) {
        return checkValidInputLevelBeforeGetOrSet(level, true, null);
    }

    /**
     * @param level                    The level for which the forward pointer is desired.
     * @param newForwardPointerAtLevel The new forward pointer to set.
     * @return The old forward pointer at the given level.
     */
    @Nullable
    public SkipListNode<K> setNext(int level, @Nullable SkipListNode<K> newForwardPointerAtLevel) {

        // assert the forward pointer key is greater than this.key
        assert newForwardPointerAtLevel == null
                || (newForwardPointerAtLevel.key != null
                && (this.key == null || newForwardPointerAtLevel.key.compareTo(this.key) > 0));
        return checkValidInputLevelBeforeGetOrSet(level, false, newForwardPointerAtLevel);
    }

    /**
     * @param key The key to be compared with the one of this instance.
     * @return false if either the key of this instance or the given parameter
     * are null, true if the key of this instance is strictly lower than the input key,
     * false otherwise.
     * @throws IllegalArgumentException if the type of the key is invalid.
     * @throws ClassCastException       if the type of the given key cannot be
     *                                  cast to the type of the key of this instance.
     */
    public boolean isKeyLowerThan(Object key) {
        // exception is thrown if invalid cast
        //noinspection unchecked
        return getKey() != null && key != null && getKey().compareTo((K) key) < 0;
    }

    /**
     * @param key The key to be compared with the one of this instance.
     * @return false if either the key of this instance or the given parameter
     * are null, true if the key of this instance is lower or equals to the input key,
     * false otherwise.
     * @throws IllegalArgumentException if the type of the key is invalid.
     * @throws ClassCastException       if the type of the given key cannot be
     *                                  cast to the type of the key of this instance.
     */
    public boolean isKeyLowerOrEqualsTo(Object key) {
        // exception is thrown if invalid cast
        //noinspection unchecked
        return getKey() != null && key != null && getKey().compareTo((K) key) <= 0;
    }

    /**
     * @param key The key to be compared with the one of this instance.
     * @return true if the key (eventually null) of this instance is equal
     * to the input key, false otherwise. If both keys are null, this method
     * returns true.
     * @throws IllegalArgumentException if the type of the key is invalid.
     * @throws ClassCastException       if the type of the given key cannot be
     *                                  cast to the type of the key of this instance.
     */
    public boolean isSameKey(Object key) {
        return Objects.equals(getKey(), key);
    }

    /**
     * Two instance of this class are considered equals if their
     * {@link #key}s are equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkipListNode<?> that = (SkipListNode<?>) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

}
