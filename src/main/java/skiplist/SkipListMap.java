package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
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
public class SkipListMap<K extends Comparable<K>, V> implements SortedMap<K, V>, Serializable, Iterable<K> {

    /**
     * The maximum value (constant, this is a parameter) to which levels
     * of nodes are capped.
     */
    public static final int MAX_LEVEL = 16;

    /**
     * The minimum value for the level, suitable as index.
     */
    static final int LOWEST_NODE_LEVEL_INCLUDED = 0;

    /**
     * The default fraction of the nodes with level i pointers that also have level i+1 pointers.
     */
    private static final double DEFAULT_P = 1.0 / 2;

    /**
     * The minimum value (excluded) for {@link #P}.
     */
    private static final double MIN_P_EXCLUDED = 0;

    /**
     * The maximum value (included) for {@link #P}.
     */
    private static final double MAX_P_INCLUDED = 1;

    /**
     * The fraction of the nodes with level i pointers that also have level i+1 pointers.
     */
    private final double P;

    /**
     * The number of elements in this list.
     */
    private int size = 0;

    /**
     * Level of the list (see description of {@link #getListLevel()}).
     */
    private int listLevel = 0;

    /**
     * The header of a skipList has forward pointers at level one through {@link #MAX_LEVEL}.
     * The forward pointers of the header at levels higher than the current maximum level of
     * the list points to null.
     */
    @SuppressWarnings("NotNullFieldNotInitialized") // initialized by initList method invoked by constructor
    @NotNull
    private SkipListNode<K, V> header;

    /**
     * Rightmost non-null nodes of this instance.
     */
    @NotNull
    private SkipListNode<K, V>[] rightmostNodes;

    /**
     * Constructor.
     *
     * @param P The fraction of the nodes with level i pointers that also have level i+1 pointers.
     */
    public SkipListMap(final double P) {
        if (MIN_P_EXCLUDED < P && P <= MAX_P_INCLUDED) {
            this.P = P;
            initList();
            assert size == 0;
            assert listLevel == 0;
            assert header != null;
        } else {
            throw new IllegalArgumentException(
                    "P value must be such that " + MIN_P_EXCLUDED + "<P<=" + MAX_P_INCLUDED);
        }
    }

    /**
     * Constructor. {@link #DEFAULT_P} value is used.
     */
    public SkipListMap() {
        this(DEFAULT_P);
    }

    /**
     * Initializes fields of this class.
     */
    private void initList() {
        header = new SkipListNode<>(null, null, MAX_LEVEL);
        //noinspection unchecked    // generic array creation
        rightmostNodes = Collections.nCopies(MAX_LEVEL, header).toArray(new SkipListNode[0]);
        listLevel = 0;
        size = 0;
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
        var nextNode = findNode(fromKey);

        final Predicate<SkipListNode<K, V>> toAdd = node -> {
            if (node == null) return false;
            assert node.getKey() != null;
            var comparison = node.getKey().compareTo(toKey);
            return lastIncluded ? comparison <= 0 : comparison < 0;
        };

        assert nextNode == null || nextNode.getKey() != null;
        if (toAdd.test(nextNode)) {
            assert nextNode != null;
            subMap.put(nextNode);
        }

        var skipListIterator = new SkipListIterator<>(nextNode);
        while (skipListIterator.hasNext()) {
            nextNode = skipListIterator.nextNode();
            assert nextNode.getKey() != null;
            if (toAdd.test(nextNode)) {
                subMap.put(nextNode);
            } else {
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
        if (size == 0) {
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
        return size;
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
        return values().contains(value);
    }

    @Nullable
    @Override
    public synchronized V get(Object key) {
        var node = findNode(key);
        return node == null ? null : node.getValue();
    }

    /**
     * @param key The key to search. It cannot be null.
     * @return The found node for the given key or null if not found.
     */
    @Nullable
    private synchronized SkipListNode<K, V> findNode(@NotNull Object key) {
        var nodeFinder = new NodeFinder<>(header);
        return nodeFinder.findNextNode(key);
    }

    @Nullable
    @Override
    public synchronized V put(@NotNull K key, V value) {    // TODO: test update of last node
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
    private V put(@NotNull K key, V value, @NotNull NodeFinder<K, V> nodeFinder) {
        @Nullable var nodeEventuallyAlreadyPresent = nodeFinder.findNextNode(key);
        assert nodeFinder.isKeyLowerOrEqualToKeyOfNode(key, nodeEventuallyAlreadyPresent);
        @NotNull var rightmostNodesLowerThanGivenKey = nodeFinder.rightmostNodes;

        V oldValue = null; // the old value (if present or null by default) must be returned (this variable saves the old value before overwriting it)
        if (nodeEventuallyAlreadyPresent != null) {
            oldValue = nodeEventuallyAlreadyPresent.getValue();
            nodeEventuallyAlreadyPresent.setValue(value);
        } else {
            // oldValue is null by default because the node at the specified key was not present.
            var randomLevel = generateRandomLevel();    // the level for the new node
            if (randomLevel > listLevel) {
                listLevel = randomLevel;    // update the level of the list if the new node to insert has a level higher than the current level list
            }
            var nodeToInsert = new SkipListNode<>(key, value, randomLevel);
            for (int level = 0; level < randomLevel; level++) {   // update pointers (actual insertion is here)
                nodeToInsert.setNext(level, rightmostNodesLowerThanGivenKey[level].getNext(level));
                rightmostNodesLowerThanGivenKey[level].setNext(level, nodeToInsert);
                if (rightmostNodesLowerThanGivenKey[level] == rightmostNodes[level]/*compare object reference*/) {   // update rightmost nodes of list
                    rightmostNodes[level] = nodeToInsert;
                }
            }
            size++;
        }
        return oldValue;
    }

    /**
     * Like {@link #put(Comparable, Object)}, but allows to add a node
     * specifying the node instead of key and value separately.
     *
     * @param node The node to add to this instance.
     * @return the previous value associated with key, or null if there
     * was no mapping for key.
     */
    public synchronized V put(@NotNull SkipListNode<K, V> node) {
        return put(Objects.requireNonNull(node.getKey()), node.getValue());
    }

    /**
     * @return a random level between 1 (included) and {@link #MAX_LEVEL} (included).
     * The random level is generated without reference to the number of elements
     * currently present in the instance.
     */
    private int generateRandomLevel() {
        int randomLevelGenerated = 1;
        while (Math.random() < P && randomLevelGenerated < MAX_LEVEL) {
            randomLevelGenerated++;
        }
        return randomLevelGenerated;
    }

    @Nullable
    @Override
    public synchronized V remove(@NotNull Object key) { // TODO: test correct update of last node

        @NotNull var nodeFinder = new NodeFinder<>(header);
        @Nullable var nodeToRemove = nodeFinder.findNextNode(key);
        @NotNull var rightmostNodesLowerThanGivenKey = nodeFinder.rightmostNodes;

        V oldValue = null; // the old value (if present or null by default) must be returned (this variable saves the old value before overwriting it)
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
            oldValue = nodeToRemove.getValue();
            // here the node is out of the list and memory can be free (in Java: garbage collector)
            size--;

            while (listLevel > 0 && header.getNext(listLevel - 1/*indexes start from 0 in Java, hence '-1'*/) == null) {
                listLevel--;
            }

        }

        return oldValue;
    }


    /**
     * Creates a copy of the given node and inserts it at the end of this instance,
     * without checking the order, which is a programmer's responsibility.
     *
     * @param node The node to be copied and whose copy has to be inserted at
     *             the end of this instance.
     */
    void copyNodeAndInsertAtEnd(@NotNull SkipListNode<K, V> node) { // TODO: test
        var nodeToInsert = new SkipListNode<>(node);
        for (int level = 0; level < node.getLevel(); level++) {   // update pointers (actual insertion is here)
            nodeToInsert.setNext(level, null/*node is added at the end of the list, there are no more nodes following this one*/);
            rightmostNodes[level].setNext(level, nodeToInsert);
            rightmostNodes[level] = nodeToInsert;
        }
        size++;
    }

    @Override
    public synchronized void clear() {
        initList();
    }

    @Override
    public synchronized void putAll(@NotNull Map<? extends K, ? extends V> m) { // TODO: test
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
    public synchronized boolean putAllKeys(@NotNull Collection<? extends K> keys) { // TODO: test
        boolean changed = false;
        if (!keys.isEmpty()) {
            var sortedKeySet = keys.stream().sorted().collect(Collectors.toList());
            var nodeFinder = new NodeFinder<>(header);
            for (var key : sortedKeySet) {
                changed = put(key, null, nodeFinder) != null || changed;
            }
        }
        return changed;
    }

    /**
     * Getter for the {@link #header}.
     */
    SkipListNode<K, V> getHeader() {
        return header;
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder(
                "SkipListMap{" +
                        "P=" + P +
                        ", size=" + size +
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
        List<V> values = new ArrayList<>();
        var iterator = new SkipListIterator<>(header);
        while (iterator.hasNext()) {
            values.add(iterator.nextNode().getValue());
        }
        return values;
    }

    @NotNull
    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entrySet = new ConcurrentSkipListSet<>(Entry.comparingByKey());
        var iterator = new SkipListIterator<>(header);
        while (iterator.hasNext()) {
            entrySet.add(iterator.nextNode());
        }
        return entrySet;
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
    synchronized SkipListNode<K, V> getFirstNodeOrNull() {
        return isEmpty() ? null : header.getNext(LOWEST_NODE_LEVEL_INCLUDED);
    }

    /**
     * Class to find a node by key exploiting the known order and the forward pointers.
     */
    private static class NodeFinder<K extends Comparable<K>, V> {

        /**
         * The array of the rightmost {@link SkipListNode}s in the list whose
         * keys are strictly lower than the given one.
         */
        @NotNull
        private final SkipListNode<K, V>[] rightmostNodes;
        /**
         * The current node, initialized with {@link #header} at the search of the search.
         */
        @NotNull
        SkipListNode<K, V> currentNode;

        /**
         * The header of the {@link SkipListMap} to which this instance refers to.
         */
        @NotNull
        SkipListNode<K, V> header;

        /**
         * Constructor.
         */
        private NodeFinder(@NotNull SkipListNode<K, V> header) {
            this.header = Objects.requireNonNull(header);
            this.currentNode = header;

            // generic array creation
            //noinspection unchecked
            this.rightmostNodes =
                    (SkipListNode<K, V>[]) Collections.nCopies(MAX_LEVEL, header).toArray(new SkipListNode[0]);
        }

        /**
         * Finds the node with the given key in the instance, starting searching from
         * the node following the {@link #currentNode}.
         *
         * @param key The key to search. It cannot be null.
         * @return The found node for the given key or null if not found.
         */
        public SkipListNode<K, V> findNextNode(@NotNull Object key) {
            //noinspection ConstantConditions   // one more assert is better than one less
            assert key != null;

            for (int level = currentNode.getLevel() - 1;        // start search from the highest level node
                 level >= LOWEST_NODE_LEVEL_INCLUDED;   // down till the lowest level or break before if node is found
                 level--) {

                /*
                 * While iterating over the list elements, saves the next node that
                 * will be examined, or null if there will not be any next node (if
                 * end of list is reached).
                 */
                @Nullable SkipListNode<K, V> nextNode;
                while ((nextNode = currentNode.getNext(level)) != null && nextNode.isKeyLowerThan(key)) {
                    currentNode = nextNode;
                }
                assert currentNode == header/*compare reference*/
                        || (currentNode.isKeyLowerThan(key)
                        && isKeyLowerOrEqualToKeyOfNode(key, currentNode.getNext(level)));

                rightmostNodes[level] = currentNode;
            }

            var nextNode = currentNode.getNext(LOWEST_NODE_LEVEL_INCLUDED);
            if (nextNode != null && nextNode.isSameKey(key)) {
                currentNode = nextNode;
                return currentNode;
            } else {
                return null;    // null if node not found
            }
        }

        /**
         * Utility method used to assert that given key is lower or equal to the key of the given node.
         *
         * @param key  The key of interest.
         * @param node The node of interest.
         * @return true if the given key results lower or equal to the key of the given node.
         */
        private boolean isKeyLowerOrEqualToKeyOfNode(Object key, SkipListNode<K, V> node) {
            // method created only to check an assertion
            Comparable<K> keyOfNode;
            //noinspection unchecked
            return node != header       // if the node is the header (same reference), given key cannot be lower
                    && key != null      // key of a non-header elements is not null hopefully
                    && (node == null    // if node is null, it means that the end of list is reached, hence given key is for sure lower
                    || ((keyOfNode = node.getKey()) != null // null key is considered to be lower than any other key
                    && keyOfNode.getClass().isAssignableFrom(key.getClass()) // check type compatibility
                    && keyOfNode.compareTo((K) key) >= 0));  // given key must be lower or equal than the key of node, i.e., key of node must be strictly greater than given key
        }
    }
}
