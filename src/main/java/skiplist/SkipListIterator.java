package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

/**
 * Iterator for {@link SkipListMap}.
 *
 * @param <K> The type of the keys.
 * @param <V> The type of the values.
 * @author Matteo Ferfoglia
 */
public class SkipListIterator<K extends Comparable<K>, V> implements Iterator<K> {
    // implicitly tested while testing SkipListMap

    /**
     * The lowest {@link SkipListNode} level.
     */
    private final static int LOWEST_NODE_LEVEL_INDEX = 0;
    /**
     * The next node pointed by this instance.
     * It is an array, because the {@link SkipListMap}
     * works over a number of levels and each {@link SkipListNode}
     * has its own level.
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    private final SkipListNode[] nextNode;
    /**
     * The current node pointed by this instance.
     */
    @Nullable
    private SkipListNode<K, V> currentNode;

    /**
     * Constructs an iterator where iterations start from the
     * node next to the given one (e.g., if you want to iterate from
     * the beginning, you have to pass the header of the {@link SkipListMap}
     * and the iterations will start from the first node following the
     * header, i.e., the header will not take part to the iterations).
     *
     * @param startingNodeExcluded The starting {@link SkipListNode} instance
     *                             such that iterations start from the node following
     *                             the given one.
     */
    public SkipListIterator(@Nullable SkipListNode<K, V> startingNodeExcluded) {
        this.currentNode = startingNodeExcluded;
        //noinspection ConstantConditions // Dereference of 'startingNodeExcluded' cannot produce 'NullPointerException' because if startingNodeExcluded then the range of the IntStream will be empty
        this.nextNode = IntStream.range(
                        LOWEST_NODE_LEVEL_INDEX,
                        startingNodeExcluded == null ? LOWEST_NODE_LEVEL_INDEX : startingNodeExcluded.getLevel())
                .mapToObj(startingNodeExcluded::getNext)
                .toArray(SkipListNode[]::new);
    }

    @Override
    public boolean hasNext() {
        return hasNext(LOWEST_NODE_LEVEL_INDEX);
    }

    /**
     * Like {@link #hasNext()}, but specific for {@link SkipListMap}s
     * and able to work with {@link SkipListNode} levels.
     *
     * @param level The level.
     * @return true if a next {@link SkipListNode} at the specified level exists, false otherwise.
     */
    public boolean hasNext(int level) {
        assert level >= LOWEST_NODE_LEVEL_INDEX;
        assert level < nextNode.length;
        return nextNode[level] != null;
    }

    @Override
    @Nullable
    public K next() {
        return next(LOWEST_NODE_LEVEL_INDEX);
    }

    /**
     * Like {@link #next()}, but moves the iteration one position ahead
     * along the specified level of the {@link SkipListMap}.
     * All the lower-level "next nodes" are updated (they will point to
     * the same {@link SkipListNode} reached by the iterator at the given
     * level).
     *
     * @param level The level at which the iterator must move one position ahead.
     * @return The next {@link SkipListNode} at the specified level.
     * @throws NoSuchElementException if there are no more {@link SkipListNode}s at the specified level.
     */
    @Nullable
    public K next(int level) {

        assert level >= LOWEST_NODE_LEVEL_INDEX;
        assert level < nextNode.length;

        if (!hasNext(level)) {
            throw new NoSuchElementException();
        }

        assert nextNode[level] != null; // otherwise hasNext had to return false
        //noinspection unchecked
        currentNode = (SkipListNode<K, V>) nextNode[level];
        nextNode[level] = nextNode[level].getNext(level);
        for (int lev = level - 1; lev >= LOWEST_NODE_LEVEL_INDEX; lev--) {
            nextNode[lev] = nextNode[level]; // update lower levels of the next node.
        }

        assert currentNode != null;
        return currentNode.getKey();
    }

    /**
     * Move the iterator one position ahead and return the resulting instance.
     *
     * @return the currently pointed {@link SkipListNode}.
     * @throws NoSuchElementException if no more elements are available.
     */
    @NotNull
    public SkipListNode<K, V> nextNode() {
        return nextNode(LOWEST_NODE_LEVEL_INDEX);
    }


    /**
     * Like {@link #nextNode()}, but moves the iterator one position ahead
     * along the specified level of the {@link SkipListMap}.
     *
     * @param level The level at which the iterator must move one position ahead.
     * @return the currently pointed {@link SkipListNode}.
     * @throws NoSuchElementException if no more elements are available.
     */
    @NotNull
    public SkipListNode<K, V> nextNode(int level) {
        next(level);
        assert currentNode != null; // next() should throw if no more elements are available
        return currentNode;
    }
}
