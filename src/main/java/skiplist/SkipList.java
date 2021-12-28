package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

import static skiplist.SkipListMap.LOWEST_NODE_LEVEL_INCLUDED;

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
     * Constructor.
     * Default missing parameters are used, as described in {@link SkipListMap#SkipListMap(double)}.
     *
     * @param P See the description of the parameter for {@link SkipListMap}.
     */
    public SkipList(double P) {
        skipListMap = new SkipListMap<>(P);
    }

    /**
     * Constructor.
     * Default missing parameters are used, as described in {@link SkipListMap#SkipListMap(int)}.
     *
     * @param maxListLevel See the description of the parameter for {@link SkipListMap}.
     */
    public SkipList(int maxListLevel) {
        skipListMap = new SkipListMap<>(maxListLevel);
    }

    /**
     * Constructor.
     *
     * @param maxListLevel See the description of the parameter for {@link SkipListMap#SkipListMap(int, double)}.
     * @param P            See the description of the parameter for {@link SkipListMap#SkipListMap(int, double)}.
     */
    public SkipList(int maxListLevel, double P) {
        skipListMap = new SkipListMap<>(maxListLevel, P);
    }

    /**
     * Default constructor.
     * Default parameters are used, as described in {@link SkipListMap#SkipListMap()}.
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
     * @param lists The lists for which the union will be computed.
     * @return a new instance with the union of the given two.
     */
    @SafeVarargs
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> union(@NotNull final SkipList<T>... lists) {

        SkipList<T> union = new SkipList<>();
        if (lists.length == 0) {
            return union;    // empty union
        }
        if (lists.length == 1) {
            union.addAll(lists[0]);
            return union;
        }

        var nodeFinders = Arrays.stream(lists)
                .map(SkipList::getHeader)
                .map(NodeFinder::new)
                .toArray(NodeFinder[]::new);
        var currentNodes = Arrays.stream(lists)
                .map(SkipList::getFirstNodeOrNull)
                .toArray(SkipListNode[]::new);

        while (Arrays.stream(currentNodes).anyMatch(Objects::nonNull)) {

            //noinspection ConstantConditions   // keys should be non-null
            @SuppressWarnings("unchecked")
            T minKey = (T) Arrays.stream(currentNodes)
                    .filter(Objects::nonNull)
                    .map(SkipListNode::getKey)
                    .min(Comparable::compareTo)
                    .orElse(null);
            assert minKey != null;    // if at least one node is non-null, hence its key should not be null

            SkipListNode<T, Object> lastAddedToUnion = null;
            for (int i = 0; i < nodeFinders.length; i++) {
                if (nodeFinders[i].findNextNode(minKey) != null) {
                    currentNodes[i] = currentNodes[i].getNext(LOWEST_NODE_LEVEL_INCLUDED);
                    //noinspection unchecked
                    var nodeGoingToAddToUnion = (SkipListNode<T, Object>) nodeFinders[i].currentNode;
                    if (!nodeGoingToAddToUnion.equals(lastAddedToUnion)) {
                        union.skipListMap.copyNodeAndInsertAtEnd(nodeGoingToAddToUnion);
                        lastAddedToUnion = nodeGoingToAddToUnion;
                    }
                }
            }
        }

        return union;
    }

    /**
     * Computes the intersection of the two instances passed as parameters
     * without modifying them.
     *
     * @param a One instance.
     * @param b The other instance.
     * @return a new instance with the intersection of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> intersection2(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b) {

        SkipList<T> intersection = new SkipList<>();
        if (a.isEmpty() || b.isEmpty()) {
            return intersection;    // empty intersection
        }

        var nodeFinderA = new NodeFinder<>(a.getHeader());
        var nodeFinderB = new NodeFinder<>(b.getHeader());

        var currentA = a.getFirstNodeOrNull();
        var currentB = b.getFirstNodeOrNull();

        while (currentA != null && currentB != null) {
            assert currentA.getKey() != null;
            assert currentB.getKey() != null;
            var comparison = currentA.getKey().compareTo(currentB.getKey());
            if (comparison == 0) {
                //noinspection unchecked    // only keys matter for SkipList
                intersection.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentA);
                currentA = currentA.getNext(LOWEST_NODE_LEVEL_INCLUDED);
                currentB = currentB.getNext(LOWEST_NODE_LEVEL_INCLUDED);
            } else if (comparison < 0) {
                var nextNode = nodeFinderA.findNextNode(currentB.getKey());
                currentA = nextNode == null ? currentA.getNext(LOWEST_NODE_LEVEL_INCLUDED) : nextNode;
            } else {
                var nextNode = nodeFinderB.findNextNode(currentA.getKey());
                currentB = nextNode == null ? currentB.getNext(LOWEST_NODE_LEVEL_INCLUDED) : nextNode;
            }
        }

        return intersection;
    }

    /**
     * Computes the intersection of the instances passed as parameters without
     * modifying them.
     *
     * @param <T>   The type of elements of the lists.
     * @param lists Array of the instances to intersect.
     * @return a new instance with the intersection of the given two.
     */
    @SafeVarargs
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> intersection(@NotNull final SkipList<T>... lists) {

        SkipList<T> intersection = new SkipList<>();
        if (lists.length == 0) {
            return intersection;    // empty intersection
        }
        if (lists.length == 1) {
            intersection.addAll(lists[0]);
            return intersection;
        }

        var nodeFinders = Arrays.stream(lists)
                .map(SkipList::getHeader)
                .map(NodeFinder::new)
                .toArray(NodeFinder[]::new);
        var currentNodes = Arrays.stream(lists)
                .map(SkipList::getFirstNodeOrNull)
                .toArray(SkipListNode[]::new);

        while (Arrays.stream(currentNodes).noneMatch(Objects::isNull)) {
            assert Arrays.stream(currentNodes).map(SkipListNode::getKey).noneMatch(Objects::isNull);

            //noinspection ConstantConditions   // keys should be non-null
            @SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"/*at least one element is present, hence a max is present for sure*/})
            T maxKey = (T) Arrays.stream(currentNodes)
                    .map(SkipListNode::getKey)
                    .max(Comparable::compareTo)
                    .get();

            if (Arrays.stream(nodeFinders).allMatch(nodeFinder -> nodeFinder.findNextNode(maxKey) != null)) {
                // node found in all lists
                //noinspection unchecked
                intersection.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) nodeFinders[0].currentNode);
            }

            // update nodes
            currentNodes = Arrays.stream(nodeFinders).sequential()
                    .map(nodeFinder -> nodeFinder.currentNode.getNext(LOWEST_NODE_LEVEL_INCLUDED))
                    .toArray(SkipListNode[]::new);
        }

        return intersection;
    }

    /**
     * See {@link SkipListMap#setMaxListLevel(int)}.
     *
     * @param maxListLevel The new value for the maxListLevel.
     * @return this instance after having updated the maxListLevel.
     * @throws IllegalArgumentException If the input parameter is too low.
     */
    public SkipList<T> setMaxListLevel(int maxListLevel) {
        skipListMap.setMaxListLevel(maxListLevel);
        return this;
    }


    /**
     * This method is similar to {@link #setMaxListLevel(int)}, but,
     * instead of taking the new value as input parameter, this method
     * uses some heuristics to choose the more adequate value according
     * to the current size of this instance.
     *
     * @return this instance after having updated the maxListLevel.
     */
    public SkipList<T> setMaxListLevel() {
        skipListMap.setMaxListLevel();
        return this;
    }

    @NotNull
    private SkipListNode<T, ?> getHeader() {
        return skipListMap.getHeader();
    }

    @Override
    public synchronized int size() {
        return skipListMap.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return skipListMap.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        //noinspection SuspiciousMethodCalls
        return skipListMap.containsKey(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return skipListMap.iterator();
    }

    @SuppressWarnings("NullableProblems")
    @NotNull
    @Override
    public synchronized Object[] toArray() {
        return skipListMap.keySet().toArray();
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @NotNull
    @Override
    public synchronized <T1> T1[] toArray(@NotNull T1[] a) {
        var size = size();
        T1[] dest = (T1[]) Array.newInstance(a.getClass().getComponentType(), size);
        var it = iterator();

        int i;
        for (i = 0; it.hasNext(); i++) {
            dest[i] = (T1) it.next();
        }
        assert i == size;

        if (dest.length > 0 && a.length > 0) {
            System.arraycopy(dest, 0, a, 0, a.length);
        }
        return dest;
    }

    @Override
    public synchronized boolean add(@NotNull T t) {
        skipListMap.put(Objects.requireNonNull(t), null);
        return true;
    }

    @Override
    public synchronized boolean remove(@NotNull Object o) {
        var old = skipListMap.remove(o);
        return old != null;
    }

    @Override
    public synchronized boolean containsAll(@NotNull Collection<?> c) {
        //noinspection SuspiciousMethodCalls
        return c.stream().unordered().filter(skipListMap::containsKey).count() == c.size();
    }

    @Override
    public synchronized boolean addAll(@NotNull Collection<? extends T> c) {
        return skipListMap.putAllKeys(c);
    }

    @Override
    public synchronized boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void clear() {
        skipListMap.clear();
    }

    @Override
    public synchronized String toString() {
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
    public synchronized boolean removeAll(@NotNull Collection<?> c) {
        boolean setChanged = false;
        for (@NotNull var e : c) {
            //noinspection SuspiciousMethodCalls
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
    public synchronized SortedSet<T> subSet(@NotNull T fromElement, @NotNull T toElement) {
        return new SkipList<>(skipListMap.subMap(
                Objects.requireNonNull(fromElement), Objects.requireNonNull(toElement)).keySet());
    }

    @NotNull
    @Override
    public synchronized SortedSet<T> headSet(T toElement) {
        return new SkipList<>(skipListMap.headMap(Objects.requireNonNull(toElement)).keySet());
    }

    @NotNull
    @Override
    public synchronized SortedSet<T> tailSet(T fromElement) {
        return new SkipList<>(skipListMap.tailMap(Objects.requireNonNull(fromElement)).keySet());
    }

    @Override
    public synchronized T first() {
        return skipListMap.firstKey();
    }

    @Override
    public synchronized T last() {
        return skipListMap.lastKey();
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
    public synchronized SkipList<T> merge(@NotNull SkipList<T> o) {
        addAll(o);
        return this;
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SkipList<?> skipList = (SkipList<?>) o;

        return skipListMap.equals(skipList.skipListMap);
    }

    @Override
    public synchronized int hashCode() {
        return skipListMap.hashCode();
    }
}
