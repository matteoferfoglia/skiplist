package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static skiplist.SkipListMap.*;

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
     * Default missing parameters are used, as described in {@link SkipListMap#SkipListMap(int)}.
     *
     * @param maxListLevel See the description of the parameter for {@link SkipListMap}.
     * @param comparator   The {@link Comparator} to use to compare the elements
     *                     of this object. If null, default comparator is used.
     */
    public SkipList(int maxListLevel, @Nullable final Comparator<T> comparator) {
        skipListMap = new SkipListMap<>(maxListLevel);
        skipListMap.setKeyComparator(comparator);
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
     * Default constructor.
     * Default parameters are used, as described in {@link SkipListMap#SkipListMap()}.
     *
     * @param comparator The {@link Comparator} to use to compare the elements
     *                   of this object. If null, default comparator is used.
     */
    public SkipList(@Nullable final Comparator<T> comparator) {
        skipListMap = new SkipListMap<>();
        skipListMap.setKeyComparator(comparator);
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
        setMaxListLevel(getBestMaxListLevelAccordingToExpectedSize(collection.size(), SkipListMap.DEFAULT_P));
        addAll((collection));
    }

    /**
     * Creates a new instance of this class with all elements from
     * the given input collection.
     *
     * @param collection The {@link Collection} from which this instance
     *                   be  created.
     * @param comparator The {@link Comparator} to use to compare the elements
     *                   of this object. If null, default comparator is used.
     */
    public SkipList(@NotNull final Collection<T> collection, @Nullable final Comparator<T> comparator) {
        skipListMap = new SkipListMap<>();
        skipListMap.setKeyComparator(comparator);
        setMaxListLevel(getBestMaxListLevelAccordingToExpectedSize(collection.size(), SkipListMap.DEFAULT_P));
        addAll(collection);
    }

    /**
     * Creates and returns a new instance of this class with all elements from
     * the given input <strong>already sorted</strong> sortedCollection.
     * <p/>
     * <strong>NOTICE: if the provided collection is not sorted, unexpected behaviours
     * might happen while using the returned data-structure.</strong>
     *
     * @param <T>              The type of the element in the input collection.
     * @param sortedCollection The <strong>sorted</strong> {@link Collection}
     *                         from which a new instance must be created
     * @param comparator       The {@link Comparator} to use to compare the elements
     *                         of the object to be created. If null, default comparator is used.
     */
    public static <T extends Comparable<T>> SkipList<T> createNewInstanceFromSortedCollection(
            @NotNull final Collection<T> sortedCollection, @Nullable final Comparator<T> comparator) {

        // check order
        assert sortedCollection.stream().sorted(comparator == null ? Comparator.naturalOrder() : comparator)
                .collect(Collectors.toList()).equals(new ArrayList<>(sortedCollection));

        var skipList = new SkipList<T>();
        skipList.skipListMap.setKeyComparator(comparator);
        skipList.setMaxListLevel(
                getBestMaxListLevelAccordingToExpectedSize(sortedCollection.size(), SkipListMap.DEFAULT_P));
        skipList.skipListMap.putAllKeysWithoutCheckingOrderAtTheEnd(sortedCollection);
        return skipList;
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

        switch (lists.length) {
            case 0:
                return new SkipList<>();  // empty union
            case 1:
                return new SkipList<>(lists[0]);
            case 2:
                return union2(lists[0], lists[1]);
            default:
                break; // continue with this method
        }

        SkipList<T> union = new SkipList<>();

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
     * without modifying them. If both input lists contains the same element
     * (same means that the given comparator} applied to the keys of the two
     * elements return 0), the elements will be added to the resulting
     * intersection only if the predicate will evaluate to true.
     * An example of use of this method is in an Information Retrieval System
     * where to answer phrasal queries might be needed to compute the
     * intersection of posting lists (saved as {@link SkipList}) with some
     * constraints on the position of terms in documents (position may be
     * an attribute of a posting instance, in the case that {@link SkipList}
     * of postings is considered.
     *
     * @param <T>             The type of elements in the list.
     * @param a               One instance.
     * @param b               The other instance.
     * @param insertPredicate The {@link BiPredicate} that two elements must
     *                        satisfy in order to be added to the resulting
     *                        intersection list.
     * @param comparator      The comparator to use.
     * @return a new instance with the intersection of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> intersection(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b,
            @NotNull final BiPredicate<@NotNull T, @NotNull T> insertPredicate,
            @NotNull final Comparator<@NotNull T> comparator) {

        SkipList<T> intersection = new SkipList<>(comparator);
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
            var comparison = comparator.compare(currentA.getKey(), currentB.getKey());
            if (comparison == 0) {
                if (insertPredicate.test(currentA.getKey(), currentB.getKey())) {
                    //noinspection unchecked    // only keys matter for SkipList
                    intersection.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentA);
                }
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
     * Computes the intersection of the two instances passed as parameters
     * without modifying them. If both input lists contains the same element
     * (same means that the {@link Comparable#compareTo(Object)} applied to
     * the keys of the two elements return 0), the elements will be added to
     * the resulting intersection only if the predicate will evaluate to true.
     * An example of use of this method is in an Information Retrieval System
     * where to answer phrasal queries might be needed to compute the
     * intersection of posting lists (saved as {@link SkipList}) with some
     * constraints on the position of terms in documents (position may be
     * an attribute of a posting instance, in the case that {@link SkipList}
     * of postings is considered.
     *
     * @param <T>             The type of elements in the list.
     * @param a               One instance.
     * @param b               The other instance.
     * @param insertPredicate The {@link BiPredicate} that two elements must
     *                        satisfy in order to be added to the resulting
     *                        intersection list.
     * @return a new instance with the intersection of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> intersection(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b,
            @NotNull final BiPredicate<@NotNull T, @NotNull T> insertPredicate) {

        return intersection(a, b, insertPredicate, Comparable::compareTo); // use natural comparator
    }

    /**
     * Computes the intersection of the two instances passed as parameters
     * without modifying them. If both input lists contains the same element
     * (same means that the given comparator applied to the keys of the two
     * elements return 0), the elements will be added to the resulting
     * intersection.
     *
     * @param <T>        The type of elements in the list.
     * @param a          One instance.
     * @param b          The other instance.
     * @param comparator The comparator to use.
     * @return a new instance with the intersection of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> intersection(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b,
            @NotNull final Comparator<@NotNull T> comparator) {

        return intersection(a, b, (o1, o2) -> true, comparator);
    }

    /**
     * Computes the unoin of the two instances passed as parameters
     * without modifying them.
     *
     * @param <T>        The type of elements in the list.
     * @param a          One instance.
     * @param b          The other instance.
     * @param comparator The comparator to use.
     * @return a new instance with the intersection of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> union(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b,
            @NotNull final Comparator<@NotNull T> comparator) {

        SkipList<T> union = new SkipList<>(
                getBestMaxListLevelAccordingToExpectedSize(a.size() + b.size(), DEFAULT_P), comparator);

        var currentA = a.getFirstNodeOrNull();
        var currentB = b.getFirstNodeOrNull();

        while (currentA != null && currentB != null) {
            assert currentA.getKey() != null;
            assert currentB.getKey() != null;
            var comparison = comparator.compare(currentA.getKey(), currentB.getKey());
            if (comparison == 0) {
                //noinspection unchecked    // only keys matter for SkipList
                union.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentA);
                currentA = currentA.getNext(LOWEST_NODE_LEVEL_INCLUDED);
                currentB = currentB.getNext(LOWEST_NODE_LEVEL_INCLUDED);
            } else if (comparison < 0) {
                //noinspection unchecked
                union.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentA);
                currentA = currentA.getNext(LOWEST_NODE_LEVEL_INCLUDED);
            } else {
                //noinspection unchecked
                union.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentB);
                currentB = currentB.getNext(LOWEST_NODE_LEVEL_INCLUDED);
            }
        }
        while (currentA != null) {    // add missing nodes from listA
            //noinspection unchecked
            union.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentA);
            currentA = currentA.getNext(LOWEST_NODE_LEVEL_INCLUDED);
        }
        while (currentB != null) {    // add missing nodes from listB
            //noinspection unchecked
            union.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentB);
            currentB = currentB.getNext(LOWEST_NODE_LEVEL_INCLUDED);
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
    private static <T extends Comparable<T>> SkipList<T> intersection2(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b) {
        return intersection(a, b, (BiPredicate<T, T>) (x, y) -> true);
    }

    /**
     * Computes the union of the two instances passed as parameters
     * without modifying them.
     *
     * @param a One instance.
     * @param b The other instance.
     * @return a new instance with the union of the given two.
     */
    @NotNull
    private static <T extends Comparable<T>> SkipList<T> union2(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b) {
        return union(a, b, Comparable::compareTo);
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

        switch (lists.length) {
            case 0:
                return new SkipList<>();  // empty intersection
            case 1:
                return new SkipList<>(lists[0]);
            case 2:
                return intersection2(lists[0], lists[1]);
            default:
                break; // continue with this method
        }

        SkipList<T> intersection = new SkipList<>();

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
     * Computes the difference of the two instances passed as parameters
     * without modifying them. If both input lists contains the same element
     * (same means that the given comparator applied to the keys of the two
     * elements return 0), the element will <strong>not</strong> be added
     * to the resulting intersection only if the predicate will evaluate to true.
     * <strong>Notice:</strong> this method is <strong>not</strong> commutative
     * wrt. its operands (input parameters).
     * <p/>
     * In other words, given two lists (let they be A and B respectively), this
     * method computes the difference between them (A\B) only if the given
     * predicate holds. Examples (p is the input {@link BiPredicate}, the result
     * of the method is shown after the arrow "&rArr;"):
     * <ul>
     *     <li>A={1,2,3,4}, B={2,3}, p=(a,b)->true &rArr; {1,4}</li>
     *     <li>A={1,2,3,4}, B={2,3}, p=(a,b)->false &rArr; {1,2,3,4}</li>
     *     <li>A={1,2,3,4}, B={2,3}, p=(a,b)->a==2 &rArr; {1,3,4}</li>
     * </ul>
     * <p/>
     *
     * @param <T>              The type of elements in the list.
     * @param a                One instance.
     * @param b                The other instance.
     * @param excludePredicate The {@link BiPredicate} that two elements must satisfy in order to
     *                         <strong>not</strong> be added to the resulting difference list.
     * @param comparator       The comparator to use.
     * @return a new instance with the intersection of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> difference(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b,
            @NotNull final BiPredicate<@NotNull T, @NotNull T> excludePredicate,
            @NotNull final Comparator<@NotNull T> comparator) {

        SkipList<T> difference = new SkipList<>(a.skipListMap.getMaxListLevel(), a.skipListMap.getP());
        if (a.isEmpty()) {
            return difference;    // empty difference
        }

        var nodeFinderA = new NodeFinder<>(a.getHeader());
        var nodeFinderB = new NodeFinder<>(b.getHeader());

        var currentA = a.getFirstNodeOrNull();
        var currentB = b.getFirstNodeOrNull();

        while (currentA != null && currentB != null) {
            assert currentA.getKey() != null;
            assert currentB.getKey() != null;
            var comparison = comparator.compare(currentA.getKey(), currentB.getKey());
            if (comparison == 0) {
                if (!excludePredicate.test(currentA.getKey(), currentB.getKey())) {
                    //noinspection unchecked    // only keys matter for SkipList
                    difference.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentA);
                }
                currentA = currentA.getNext(LOWEST_NODE_LEVEL_INCLUDED);
                currentB = currentB.getNext(LOWEST_NODE_LEVEL_INCLUDED);
            } else if (comparison < 0) {
                //noinspection unchecked
                difference.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentA);
                currentA = currentA.getNext(LOWEST_NODE_LEVEL_INCLUDED);
            } else {
                var nextNode = nodeFinderB.findNextNode(currentA.getKey());
                currentB = nextNode == null ? currentB.getNext(LOWEST_NODE_LEVEL_INCLUDED) : nextNode;
            }
        }
        while (currentA != null) {    // add missing nodes from listA
            //noinspection unchecked
            difference.skipListMap.copyNodeAndInsertAtEnd((SkipListNode<T, Object>) currentA);
            currentA = currentA.getNext(LOWEST_NODE_LEVEL_INCLUDED);
        }

        return difference;
    }

    /**
     * Computes the difference of the two instances passed as parameters
     * without modifying them. If both input lists contains the same element
     * (same means that the {@link Comparable#compareTo(Object)} applied to
     * the keys of the two elements return 0), the elements will <strong>not</strong>
     * be added to the resulting list only if the predicate will evaluate to true.
     *
     * @param <T>              The type of elements in the list.
     * @param a                One instance.
     * @param b                The other instance.
     * @param excludePredicate The {@link BiPredicate} that two elements must satisfy in order to
     *                         <strong>not</strong> be added to the resulting difference list.
     * @return a new instance with the intersection of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> difference(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b,
            @NotNull final BiPredicate<@NotNull T, @NotNull T> excludePredicate) {

        return difference(a, b, excludePredicate, Comparable::compareTo); // use natural comparator
    }

    /**
     * Computes the difference of the two instances passed as parameters
     * without modifying them. If both input lists contains the same element
     * (same means that the given comparator applied to the keys of the two
     * elements return 0), the elements will be <strong>not</strong>added to
     * the resulting difference.
     *
     * @param <T>        The type of elements in the list.
     * @param a          One instance.
     * @param b          The other instance.
     * @param comparator The comparator to use.
     * @return a new instance with the intersection of the given two.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> difference(
            @NotNull final SkipList<T> a, @NotNull final SkipList<T> b,
            @NotNull final Comparator<@NotNull T> comparator) {

        return difference(a, b, (o1, o2) -> true, comparator);
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
        skipListMap.put((t), null);
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
        return skipListMap.comparator();
    }

    @NotNull
    @Override
    public synchronized SortedSet<T> subSet(@NotNull T fromElement, @NotNull T toElement) {
        return new SkipList<>(skipListMap.subMap(
                (fromElement), (toElement)).keySet());
    }

    @NotNull
    @Override
    public synchronized SortedSet<T> headSet(T toElement) {
        return new SkipList<>(skipListMap.headMap((toElement)).keySet());
    }

    @NotNull
    @Override
    public synchronized SortedSet<T> tailSet(T fromElement) {
        return new SkipList<>(skipListMap.tailMap((fromElement)).keySet());
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
