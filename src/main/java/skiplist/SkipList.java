package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Implementation of a SkipList, using the keySet of a {@link SkipListMap}.
 */
public class SkipList<T extends Comparable<T>> implements SortedSet<T>, Serializable {

    /**
     * The {@link SkipListMap} from which this list is created.
     */
    @NotNull
    private final SkipListMap<T, Object> skipListMap;

    /**
     * @param P See the description of the parameter for {@link SkipListMap}.
     */
    public SkipList(double P) {
        skipListMap = new SkipListMap<>(P);
    }

    /**
     * Default constructor
     */
    public SkipList() {
        skipListMap = new SkipListMap<>();
    }

    /**
     * Creates a new instance of this class with all elements from
     * the given input collection.
     *
     * @param collection The {@link Collection} from which this instance
     *                   be  created.
     */
    public SkipList(@NotNull final Collection<T> collection) {
        skipListMap = new SkipListMap<>();
        addAll(Objects.requireNonNull(collection));
    }

    /**
     * Computes the union of the instances passed as parameters without
     * modifying them.
     * This method is very similar to {@link #addAll(Collection)}, but this is
     * specific for this class, hence this is better for performance,
     * and does NOT alter any of the input parameters.
     *
     * @param a One instance.
     * @param b The other instance.
     * @return a new instance with the union of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> union(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b) {   // TODO : test
        SkipList<T> union = new SkipList<>();
        union.addAll(a);
        union.addAll(b);
        return union;
    }

    /**
     * Computes the intersection of the instances passed as parameters without
     * modifying them.
     *
     * @param a One instance.
     * @param b The other instance.
     * @return a new instance with the intersection of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> intersection(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b) {   // TODO: test

        SkipList<T> intersection = new SkipList<>();
        var currentA = a.getFirstNodeOrNull();
        var currentB = b.getFirstNodeOrNull();

        // return the next node for intersection
        TriFunction<@NotNull SkipList<T>, @NotNull SkipListNode<T, ?>, @NotNull T, @Nullable SkipListNode<T, ?>>
                nextNodeGetterOrNullIfEndOfListReached = (skipList, currentNode, maxKeyIncluded) -> {
            assert currentNode.getKey() != null;
            for (int l = currentNode.getLevel() - 1; l >= 0; l--) {
                var nextAtLevel = currentNode.getNext(l);
                if (nextAtLevel != null) {
                    assert nextAtLevel.getKey() != null;
                    var innerComparison = nextAtLevel.getKey().compareTo(maxKeyIncluded);
                    if (innerComparison <= 0) {
                        return nextAtLevel;
                    }
                }
            }
            return null;
        };

        while (currentA != null && currentB != null) {
            assert currentA.getKey() != null;
            assert currentB.getKey() != null;
            var comparison = currentA.getKey().compareTo(currentB.getKey());
            if (comparison == 0) {
                //noinspection unchecked    // only keys matter for SkipList
                intersection.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentA);
                currentA = currentA.getNext(SkipListMap.LOWEST_NODE_LEVEL_INCLUDED);
                currentB = currentB.getNext(SkipListMap.LOWEST_NODE_LEVEL_INCLUDED);
            } else if (comparison < 0) {
                currentA = nextNodeGetterOrNullIfEndOfListReached.apply(a, currentA, currentB.getKey());
            } else {
                currentB = nextNodeGetterOrNullIfEndOfListReached.apply(b, currentB, currentA.getKey());
            }
        }

        return intersection;
    }

    @Override
    public int size() {
        return skipListMap.size();
    }

    @Override
    public boolean isEmpty() {
        return skipListMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return skipListMap.containsKey(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return skipListMap.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() { // TODO: test
        return skipListMap.entrySet().toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) { // TODO: test
        return skipListMap.entrySet().toArray((T1[]) Array.newInstance(a.getClass(), 0));
    }

    @Override
    public boolean add(@NotNull T t) {
        skipListMap.put(Objects.requireNonNull(t), null);
        return true;
    }

    @Override
    public boolean remove(@NotNull Object o) {
        var old = skipListMap.remove(o);
        return old != null;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {  // TODO: test
        return c.stream().unordered().filter(e -> skipListMap.containsKey(c)).count() == c.size();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) { // TODO: test
        return skipListMap.putAllKeys(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        skipListMap.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(
                "SkipList{P=" + skipListMap.getP()
                        + ", size=" + size()
                        + ", listLevel=" + skipListMap.getListLevel()
                        + ", headerForwardsTo: " + skipListMap.getHeader().getForwardPointersKeys()
                        + ", \n\tnodes=[");

        var nextNode = skipListMap.getHeader().getNext(0);
        for (int i = 0; nextNode != null; i++) {
            sb.append("\n\t\t").append(i + 1).append(":\t").append("{value: ").append(nextNode.getKey())
                    .append(", forwardsTo: ").append(nextNode.getForwardPointersKeys());
            nextNode = nextNode.getNext(0);
        }
        sb.append("\n\t]}");

        return sb.toString();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean setChanged = false;
        for (var e : c) {
            setChanged = skipListMap.remove(e) != null || setChanged;
        }
        return setChanged;
    }

    @Nullable
    @Override
    public Comparator<? super T> comparator() {
        return null;
    }

    @NotNull
    @Override
    public SortedSet<T> subSet(@NotNull T fromElement, @NotNull T toElement) {
        return new SkipList<>(skipListMap.subMap(
                Objects.requireNonNull(fromElement), Objects.requireNonNull(toElement)).keySet());
    }

    @NotNull
    @Override
    public SortedSet<T> headSet(T toElement) {
        return new SkipList<>(skipListMap.headMap(Objects.requireNonNull(toElement)).keySet());
    }

    @NotNull
    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return new SkipList<>(skipListMap.tailMap(Objects.requireNonNull(fromElement)).keySet());
    }

    @Override
    public T first() {
        return skipListMap.firstKey();
    }

    @Override
    public T last() {
        return skipListMap.lastKey();
    }

    /**
     * @return the level of this list.
     */
    private int getListLevel() {
        return skipListMap.getListLevel();
    }

    /**
     * @return the first node of this instance (the one which follows the
     * header) or null if this instance is empty.
     */
    @Nullable
    private synchronized SkipListNode<T, ?> getFirstNodeOrNull() {
        return skipListMap.getFirstNodeOrNull();
    }

    /**
     * Merges this instance with the one given as parameter and returns this instance
     * after the invocation of this method.
     *
     * @param o The other instance of this class to be merged with this one.
     * @return this instance after merging.
     */
    public SkipList<T> merge(@NotNull SkipList<T> o) {
        // TODO: implement before in SkipListMap
        throw new UnsupportedOperationException();
    }

}
