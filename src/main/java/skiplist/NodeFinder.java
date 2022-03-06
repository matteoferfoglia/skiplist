package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import static skiplist.SkipListMap.LOWEST_NODE_LEVEL_INCLUDED;

/**
 * Class to find a node by key exploiting the known order and the forward pointers.
 */
class NodeFinder<K extends Comparable<K>, V> {
    /**
     * The header of the {@link SkipListMap} to which this instance refers to.
     */
    @NotNull
    final
    SkipListNode<K, V> header;
    /**
     * The current node, initialized with {@link #header} at the search of the search.
     */
    @NotNull
    SkipListNode<K, V> currentNode;

    /**
     * Constructor.
     */
    NodeFinder(@NotNull SkipListNode<K, V> header) {
        this.header = header;
        this.currentNode = header;
    }

    /**
     * Finds the node with the given key in the instance, starting searching from
     * the node following the {@link #currentNode} (excluded).
     * If the given key is equal to the current node key, then this method
     * will throw an assertion error (it is a pre-condition that current node
     * key must be lower than given key).
     *
     * @param key The key to search. It cannot be null.
     * @return The found node for the given key or null if not found.
     */
    @Nullable
    public SkipListNode<K, V> findNextNode(@NotNull Object key) {
        //noinspection ConstantConditions   // one more assert is better than one less
        assert key != null;
        assert !currentNode.isSameKey(key, Comparator.naturalOrder());

        for (int level = currentNode.getLevel() - 1;    // start search from the highest level node
             level >= LOWEST_NODE_LEVEL_INCLUDED;       // down till the lowest level or break before if node is found
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
                    || (currentNode.isKeyLowerThan(key, Comparator.naturalOrder())
                    && isKeyLowerOrEqualToKeyOfNode(key, currentNode.getNext(level)));
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
                && node.getKeyComparator().compare(node.getKey(), (K) key) >= 0));  // given key must be less than or equal to the key of node, i.e., key of node must be strictly greater than given key
    }
}
