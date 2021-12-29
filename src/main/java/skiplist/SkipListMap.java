package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementation of a SkipList, as described in "Skip Lists: A Probabilistic Alternative
 * to Balanced Trees", by William Pugh.
 * <p>
 * This class is an implementation of {@link SortedMap} rather than {@link List}, because
 * elements have a key and a value.
 *
 * @param <K> The type of the key of each node of this instance.
 * @param <V> The type of the value of each node of this instance.
 * @author Matteo Ferfoglia
 */
@SuppressWarnings("UnusedReturnValue")
// returned values of some methods are not used in project, but they can be useful
public class SkipListMap<K extends Comparable<K>, V> implements SortedMap<K, V>, Externalizable, Iterable<K> {

    public static final int MIN_ALLOWED_LIST_LEVEL = 1;
    /**
     * The minimum value for the level, suitable as index.
     */
    static final int LOWEST_NODE_LEVEL_INCLUDED = 0;
    /**
     * The default fraction of the nodes with level i pointers that also have level i+1 pointers.
     */
    static final double DEFAULT_P = 1.0 / 2;
    /**
     * The default maximum value for {@link #maxListLevel}.
     */
    private static final int DEFAULT_MAX_LEVEL = 16;
    /**
     * The minimum value (excluded) for {@link #P}.
     */
    private static final double MIN_P_EXCLUDED = 0;

    /**
     * The maximum value (included) for {@link #P}.
     */
    private static final double MAX_P_INCLUDED = 1;

    /**
     * The maximum value (constant, this is a parameter) to which levels
     * of nodes are capped for this instance.
     */
    private int maxListLevel;

    /**
     * The fraction of the nodes with level i pointers that also have level i+1 pointers.
     */
    private double P;

    /**
     * Level of the list (see description of {@link #getListLevel()}).
     */
    private int listLevel = 0;

    /**
     * The header of a skipList has forward pointers at level one through {@link #maxListLevel}.
     * The forward pointers of the header at levels higher than the current maximum level of
     * the list points to null.
     */
    @SuppressWarnings("NotNullFieldNotInitialized") // initialized by initInstance method invoked by constructor
    @NotNull
    private SkipListNode<K> header;

    /**
     * The actual data-structure where data are saved.
     */
    @SuppressWarnings("NotNullFieldNotInitialized") // initialized by initInstance method invoked by constructor
    @NotNull
    private HashMap<K, V> actualDataStructure;

    /**
     * Rightmost non-null nodes of this instance.
     */
    @NotNull
    private SkipListNode<K>[] rightmostNodes;

    /**
     * Constructor.
     *
     * @param maxListLevel The maximum allowed level for this instance.
     * @param P            The fraction of the nodes with level i pointers that also have level i+1 pointers.
     */
    public SkipListMap(final int maxListLevel, final double P) {
        if (MIN_ALLOWED_LIST_LEVEL <= maxListLevel && MIN_P_EXCLUDED < P && P <= MAX_P_INCLUDED) {
            this.maxListLevel = maxListLevel;
            this.P = P;
            initInstance();
            assert size() == 0;
            assert listLevel == 0;
        } else {
            throw new IllegalArgumentException(
                    "The list level must be >=0 and " +
                            "P value must be such that " + MIN_P_EXCLUDED + "<P<=" + MAX_P_INCLUDED);
        }
    }

    /**
     * Constructor.
     *
     * @param maxListLevel The maximum allowed level for this instance.
     */
    public SkipListMap(final int maxListLevel) {
        this(maxListLevel, DEFAULT_P);
    }

    /**
     * Constructor.
     *
     * @param P The fraction of the nodes with level i pointers that also have level i+1 pointers.
     */
    public SkipListMap(final double P) {
        this(DEFAULT_MAX_LEVEL, P);
    }

    /**
     * Constructor. {@link #DEFAULT_MAX_LEVEL} and {@link #DEFAULT_P} values are used.
     */
    public SkipListMap() {
        this(DEFAULT_MAX_LEVEL, DEFAULT_P);
    }

    /**
     * @param expectedSize The expected size for an instance of this class.
     * @param P            The {@link #P} parameter value for the instance.
     * @return the best value for {@link  #maxListLevel} according to some heuristics.
     */
    static int getBestMaxListLevelAccordingToExpectedSize(int expectedSize, double P) {
        return Math.max(MIN_ALLOWED_LIST_LEVEL, (int) Math.round(log(1 / P, expectedSize)));
    }

    /**
     * @param base The base of the logarithm.
     * @param n    The argument for the logarithm.
     * @return the log in the specified base of the given value.
     */
    private static double log(double base, int n) {
        return Math.log(n) / Math.log(base);
    }

    /**
     * Initializes fields of this class.
     */
    private synchronized void initInstance() {
        actualDataStructure = new HashMap<>();
        header = new SkipListNode<>((K) null, maxListLevel);
        //noinspection unchecked    // generic array creation
        rightmostNodes = Collections.nCopies(maxListLevel, header).toArray(new SkipListNode[0]);
        listLevel = 0;
    }

    /**
     * Getter for {@link #P}.
     */
    public double getP() {
        return P;
    }

    /**
     * @return the level (indexed 0 through i-1) of this instance (the level is 0
     * if the list is empty).
     */
    public synchronized int getListLevel() {
        return listLevel;
    }

    /**
     * @return the {@link #maxListLevel}.
     */
    public int getMaxListLevel() {
        return maxListLevel;
    }

    /**
     * Setter for the {@link #maxListLevel}. This method is very expensive,
     * because updating the {@link #maxListLevel} implies re-create the entire instance
     * with the new list level.
     * Choosing an adequate {@link #maxListLevel} can lead to better performance
     * when using this data structure.
     *
     * @param maxListLevel The new value for {@link #maxListLevel}.
     * @return this instance after having updated the {@link #maxListLevel}.
     * @throws IllegalArgumentException If the input parameter is lower than {@link #MIN_ALLOWED_LIST_LEVEL}.
     */
    public SkipListMap<K, V> setMaxListLevel(int maxListLevel) throws IllegalArgumentException {
        if (maxListLevel < MIN_ALLOWED_LIST_LEVEL) {
            throw new IllegalArgumentException("Minimum value for the input is: " + MIN_ALLOWED_LIST_LEVEL);
        }
        if (maxListLevel != this.maxListLevel) {
            this.maxListLevel = maxListLevel;
            SkipListMap<K, V> tmp = new SkipListMap<>();
            tmp.putAll(this);
            initInstance();
            putAll(tmp);
        }
        return this;
    }

    /**
     * This method is similar to {@link #setMaxListLevel(int)}, but,
     * instead of taking the new value as input parameter, this method
     * uses some heuristics to choose the more adequate value according
     * to the current size of this instance.
     *
     * @return this instance after having updated the {@link #maxListLevel}.
     */
    public SkipListMap<K, V> setMaxListLevel() {
        return setMaxListLevel(getBestMaxListLevelAccordingToExpectedSize(size(), P));
    }

    @Nullable
    @Override
    public Comparator<? super K> comparator() {
        return null;    // use natural ordering for keys
    }

    @NotNull
    @Override
    public synchronized SortedMap<K, V> subMap(@NotNull K fromKey, @NotNull K toKey) {
        return subMapLastIncluded(fromKey, toKey, false);
    }

    /**
     * Similar to {@link #subMap(Comparable, Comparable)}, but this
     * method allows deciding if the last key must be included in the returned
     * {@link Map}.
     *
     * @param fromKey      Initial key (included).
     * @param toKey        Last key, eventually included, depending on the third parameter.
     * @param lastIncluded If true, the element corresponding to the last key
     *                     will be included, otherwise will not.
     * @return the sub-map with nodes corresponding to the desired keys.
     */
    @NotNull
    private synchronized SkipListMap<K, V> subMapLastIncluded(@NotNull K fromKey, @NotNull K toKey, boolean lastIncluded) {
        SkipListMap<K, V> subMap = new SkipListMap<>(P);
        var nodeFinder = new NodeFinder<>(header);
        var nextNode = nodeFinder.findNextNode(fromKey);
        final Predicate<SkipListNode<K>> toAdd = node -> {
            if (node == null) return false;
            assert node.getKey() != null;
            var comparison = node.getKey().compareTo(toKey);
            return lastIncluded ? comparison <= 0 : comparison < 0;
        };
        final Function<SkipListNode<K>, Boolean> putNodeAndReturnFalseIfNoMoreNodeMustBeAdded = node -> {
            assert node != null && node.getKey() != null;
            boolean shouldAdd = toAdd.test(node);
            if (shouldAdd) {
                var key = node.getKey();
                subMap.put(key, actualDataStructure.get(key));
            }
            return shouldAdd;
        };

        putNodeAndReturnFalseIfNoMoreNodeMustBeAdded.apply(nextNode);

        var skipListIterator = new SkipListIterator<>(nextNode);
        while (skipListIterator.hasNext()) {
            nextNode = skipListIterator.nextNode();
            assert nextNode.getKey() != null;
            if (!putNodeAndReturnFalseIfNoMoreNodeMustBeAdded.apply(nextNode)) {
                break;  // list is sorted: if here, no more node have to be added in the sublist
            }
        }

        return subMap;
    }

    @NotNull
    @Override
    public synchronized SortedMap<K, V> headMap(@NotNull K toKey) {
        return subMap(firstKey(), toKey);
    }

    @NotNull
    @Override
    public synchronized SortedMap<K, V> tailMap(@NotNull K fromKey) {
        return subMapLastIncluded(fromKey, lastKey(), true);
    }

    @NotNull
    @Override
    public synchronized K firstKey() {   // implicitly tested with tailMap(..) / headMap(..)
        if (size() == 0) {
            throw new NoSuchElementException();
        } else {
            final int FIRST_NODE_LEVEL_INDEX = 0;
            var firstNode = header.getNext(FIRST_NODE_LEVEL_INDEX);
            assert firstNode != null;   // if null, size had to be 0
            assert firstNode.getKey() != null;
            return firstNode.getKey();
        }
    }

    @NotNull
    @Override
    public synchronized K lastKey() {   // implicitly tested with tailMap(..) / headMap(..)
        var skipListIterator = new SkipListIterator<>(header);
        var lastKey = skipListIterator.next();  // throw if empty
        while (skipListIterator.hasNext()) {
            lastKey = skipListIterator.next();
        }
        assert lastKey != null;
        return lastKey;
    }

    @Override
    public synchronized int size() {
        return actualDataStructure.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return findNode(key) != null;
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        //noinspection SuspiciousMethodCalls
        return values().contains(value);
    }

    @Nullable
    @Override
    public synchronized V get(Object key) {
        return actualDataStructure.get(key);
    }

    /**
     * @param key The key to search. It cannot be null.
     * @return The found node for the given key or null if not found.
     */
    @Nullable
    private synchronized SkipListNode<K> findNode(@NotNull Object key) {
        var nodeFinder = new NodeFinder<>(header);
        return nodeFinder.findNextNode(key);
    }

    @Nullable
    @Override
    public synchronized V put(@NotNull K key, V value) {
        return put(key, value, new NodeFinder<>(header));
    }

    /**
     * Put the element with given key and value to this instance.
     * If the key already exists, the corresponding element in this instance
     * will be replaced.
     * This method takes a {@link NodeFinder} instance parameter: this allows to
     * exploit the order of the collection: e.g., if you have to add a list of sorted
     * {@link SkipListNode}s to this instance, it would have no sense to create
     * each time a new instance of {@link NodeFinder} and search for the correct
     * position where to add the next {@link SkipListNode} of the input list from
     * the beginning of this instance, because, exploiting the sorting order, you
     * know that for sure the insertion position will not be previous than the position
     * in this instance of the {@link SkipListNode} from the input list that you have
     * already added. For this purpose, the method {@link NodeFinder#findNextNode(Object)}
     * is used.
     *
     * @return the old value corresponding to the given key.
     */
    @Nullable
    private synchronized V put(@NotNull K key, V value, @NotNull NodeFinder<K> nodeFinder) {
        @Nullable var nodeEventuallyAlreadyPresent = nodeFinder.findNextNode(key);
        @NotNull var rightmostNodesLowerThanGivenKey = nodeFinder.rightmostNodes;

        V oldValue = null; // the old value (if present or null by default) must be returned (this variable saves the old value before overwriting it)
        if (nodeEventuallyAlreadyPresent != null) {
            oldValue = actualDataStructure.replace(key, value);
        } else {
            // oldValue is null by default because the node at the specified key was not present.
            var randomLevel = generateRandomLevel();    // the level for the new node
            if (randomLevel > listLevel) {
                listLevel = randomLevel;    // update the level of the list if the new node to insert has a level higher than the current level list
            }
            actualDataStructure.put(key, value);
            var nodeToInsert = new SkipListNode<>(key, randomLevel);
            for (int level = 0; level < randomLevel; level++) {   // update pointers (actual insertion is here)
                var forwardPointerToSetForThisLevel = rightmostNodesLowerThanGivenKey[level].getNext(level);
                forwardPointerToSetForThisLevel =
                        forwardPointerToSetForThisLevel != null && nodeToInsert.isKeyLowerThan(forwardPointerToSetForThisLevel.getKey())  // forward pointer cannot have key lower than node key
                                ? forwardPointerToSetForThisLevel
                                : null;
                nodeToInsert.setNext(level, forwardPointerToSetForThisLevel);
                rightmostNodesLowerThanGivenKey[level].setNext(level, nodeToInsert);
                if (rightmostNodesLowerThanGivenKey[level] == rightmostNodes[level]/*compare object reference*/) {   // update rightmost nodes of list
                    rightmostNodes[level] = nodeToInsert;
                }
            }
        }
        return oldValue;
    }

    /**
     * @return a random level between 1 (included) and {@link #maxListLevel} (included).
     * The random level is generated without reference to the number of elements
     * currently present in the instance.
     */
    private int generateRandomLevel() {
        int randomLevelGenerated = 1;
        while (Math.random() < P && randomLevelGenerated < maxListLevel) {
            randomLevelGenerated++;
        }
        return randomLevelGenerated;
    }

    @Nullable
    @Override
    public synchronized V remove(@NotNull Object key) {

        var oldValue = actualDataStructure.remove(key);
        if (oldValue == null) {    // node was not present
            return null;
        }

        @NotNull var nodeFinder = new NodeFinder<>(header);
        @Nullable var nodeToRemove = nodeFinder.findNextNode(key);
        @NotNull var rightmostNodesLowerThanGivenKey = nodeFinder.rightmostNodes;

        if (nodeToRemove != null /*node found*/) {

            for (int level = 0; level < listLevel; level++) {   // update pointers (actual deletion is here)
                var rightmostNodeThisLevelBeforeTheOneToRemove = rightmostNodesLowerThanGivenKey[level];
                if (rightmostNodeThisLevelBeforeTheOneToRemove.getNext(level) != nodeToRemove) {
                    break;
                } else {
                    if (nodeToRemove == rightmostNodes[level]/*compare object reference*/) {   // update rightmost nodes of list
                        rightmostNodes[level] = rightmostNodeThisLevelBeforeTheOneToRemove;
                        rightmostNodeThisLevelBeforeTheOneToRemove.setNext(level, null);
                    } else {
                        var nodeFollowingTheOneToRemoveAtThisLevel = nodeToRemove.getNext(level);
                        rightmostNodeThisLevelBeforeTheOneToRemove.setNext(level, nodeFollowingTheOneToRemoveAtThisLevel);
                    }
                }
            }
            // here the node is out of the list and memory can be free (in Java: garbage collector)

            while (listLevel > 0 && header.getNext(listLevel - 1/*indexes start from 0 in Java, hence '-1'*/) == null) {
                listLevel--;
            }

        }

        return oldValue;
    }

    /**
     * Creates a node and inserts it at the end of this instance,
     * without checking the order, which is a programmer's responsibility.
     *
     * @param key   The key.
     * @param value The value.
     */
    void insertNodeAtEnd(@NotNull K key, @Nullable V value) {
        assert !actualDataStructure.containsKey(key);
        var previousValue = actualDataStructure.put(key, value);
        if (previousValue != null) {    // if the key was already present, the previous state must be restored
            actualDataStructure.put(key, previousValue);
            throw new IllegalStateException("The key " + key + " was already present. No change were made.");
        } else {
            var newLevelForNode = generateRandomLevel();    // nodeLevel is correlated with listLevel
            var nodeToInsert = new SkipListNode<>(key, newLevelForNode);
            if (newLevelForNode > listLevel) {
                listLevel = newLevelForNode;    // update the level of the list if the new node to insert has a level higher than the current level list
            }
            assert nodeToInsert.getLevel() <= maxListLevel && nodeToInsert.getLevel() <= listLevel;
            for (int level = 0; level < newLevelForNode; level++) {   // update pointers (actual insertion is here)
                nodeToInsert.setNext(level, null/*node is added at the end of the list, there are no more nodes following this one*/);
                rightmostNodes[level].setNext(level, nodeToInsert);
                rightmostNodes[level] = nodeToInsert;
            }
        }
    }

    @Override
    public synchronized void clear() {
        initInstance();
    }

    @Override
    public synchronized void putAll(@NotNull Map<? extends K, ? extends V> m) {
        if (!m.isEmpty()) {
            var sortedEntrySet =
                    m.entrySet().stream().sorted(Entry.comparingByKey()).collect(Collectors.toList());
            var nodeFinder = new NodeFinder<>(header);
            for (var entry : sortedEntrySet) {
                put(entry.getKey(), entry.getValue(), nodeFinder);
            }
        }
    }

    /**
     * Similar to {@link #putAll(Map)}, but this method adds all input keys
     * to this instance and set to null the corresponding values.
     * If one of the input key already exists in this instance, the
     * corresponding node will be replaced.
     *
     * @param keys Input keys to add to this instance.
     * @return true if this instance has changed as consequence of the invocation of
     * this method.
     */
    public synchronized boolean putAllKeys(@NotNull Collection<? extends K> keys) {
        boolean changed = false;
        var initialSize = size();
        if (!keys.isEmpty()) {
            var sortedKeySet = keys.stream().sorted().distinct().collect(Collectors.toList());
            var nodeFinder = new NodeFinder<>(header);
            for (var key : sortedKeySet) {
                changed = put(key, null, nodeFinder) != null || changed;
            }
        }
        return changed || size() != initialSize;
    }

    /**
     * Getter for the {@link #header}.
     */
    @NotNull
    SkipListNode<K> getHeader() {
        return header;
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder(
                "SkipListMap{" +
                        "P=" + P +
                        ", size=" + size() +
                        ", listLevel=" + listLevel +
                        ", header=" + header +
                        ", \n\tnodes=[");

        var nextNode = header.getNext(LOWEST_NODE_LEVEL_INCLUDED);
        for (int i = 0; nextNode != null; i++) {
            sb.append("\n\t\t").append(i + 1).append(":\t").append(nextNode);
            nextNode = nextNode.getNext(LOWEST_NODE_LEVEL_INCLUDED);
        }
        sb.append("\n\t]}");

        return sb.toString();
    }

    @NotNull
    @Override
    public synchronized Set<K> keySet() {
        Set<K> keySet = new ConcurrentSkipListSet<>();
        for (var key : this) {
            keySet.add(key);
        }
        return keySet;
    }

    @NotNull
    @Override
    public synchronized Collection<V> values() {
        return actualDataStructure.values();
    }

    @NotNull
    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        return actualDataStructure.entrySet();
    }

    @NotNull
    @Override
    public synchronized Iterator<K> iterator() {
        return new SkipListIterator<>(header);
    }

    /**
     * @return the first node of this instance (the one which follows the
     * header) or null if this instance is empty.
     */
    @Nullable
    synchronized SkipListNode<K> getFirstNodeOrNull() {
        return isEmpty() ? null : header.getNext(LOWEST_NODE_LEVEL_INCLUDED);
    }

    /**
     * Two instances are considered equals if they have the same size and
     * the same elements in the same order.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkipListMap<?, ?> that = (SkipListMap<?, ?>) o;
        if (!header.equals(that.header)) return false;
        return actualDataStructure.equals(that.actualDataStructure);
    }

    @Override
    public int hashCode() {
        return actualDataStructure.hashCode();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(P);
        out.writeObject(maxListLevel);
        out.writeObject(listLevel);
        out.writeObject(size());
        out.writeObject(actualDataStructure);
        var it = new SkipListIterator<>(header);
        while (it.hasNext()) {
            var node = it.nextNode();
            out.writeObject(node.getKey());
        }
        out.flush();
    }

    @SuppressWarnings("unchecked")  // casting for correct deserialization
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // Read in the same order they were written
        P = (double) in.readObject();
        maxListLevel = (int) in.readObject();
        initInstance();
        listLevel = (int) in.readObject();
        var size = (int) in.readObject();
        actualDataStructure = (HashMap<K, V>) in.readObject();
        var nodeFinder = new NodeFinder<>(header);
        for (int i = 0; i < size; i++) {
            K key = (K) in.readObject();
            V value = actualDataStructure.get(key);
            put(key, value, nodeFinder);
        }
    }
}
