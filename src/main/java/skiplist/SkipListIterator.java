package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
     * The current node pointed by this instance.
     */
    @Nullable
    private SkipListNode<K, V> currentNode;
    /**
     * The next node pointed by this instance.
     */
    @Nullable
    private SkipListNode<K, V> nextNode;

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
        this.nextNode = startingNodeExcluded == null ? null : startingNodeExcluded.getNext(LOWEST_NODE_LEVEL_INDEX);
    }

    @Override
    public boolean hasNext() {
        return nextNode != null;
    }

    @Override
    @Nullable
    public K next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        assert nextNode != null;    // if null, hasNext had to return false
        currentNode = nextNode;
        nextNode = nextNode.getNext(LOWEST_NODE_LEVEL_INDEX);

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
        next();
        assert currentNode != null; // next() should throw if no more elements are available
        return currentNode;
    }
}
