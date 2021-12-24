package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Implementation of a SkipList, using the keySet of a {@link SkipListMap}.
 */
public class SkipList<T extends Comparable<T>> implements List<T>, Serializable {

    /**
     * The {@link SkipListMap} from which this list is created.
     */
    @NotNull
    private final SkipListMap<T, ?> skipListMap;

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
        boolean collectionWillChange = !containsAll(c);
        if (collectionWillChange) {
            c.stream().sorted().forEach(this::add);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        skipListMap.clear();
    }

    @Nullable
    @Override
    public T get(int index) {   // TODO: test
        if ((index < 0 || index >= size())) {
            throw new IndexOutOfBoundsException();
        } else {
            SkipListIterator<T, ?> iterator = (SkipListIterator<T, ?>) skipListMap.iterator();
            T currentValue;
            int i = 0;
            while ((currentValue = iterator.next()) != null && i < index) {
                i++;
            }

            assert i - 1 == index;
            return currentValue;
        }
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException();  // cannot set at specific position, order is decided by the data-structure
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException();  // cannot set at specific position, order is decided by the data-structure
    }

    @Nullable
    @Override
    public T remove(int index) {
        @Nullable var keyAtIndex = get(index);
        if (keyAtIndex != null) {
            skipListMap.remove(keyAtIndex);
        }
        return keyAtIndex;
    }

    @Override
    public int indexOf(Object o) {  // TODO: test (must respect its contract)
        int i = 0;
        for (var k : skipListMap) {
            if (k.equals(o)) {
                break;
            } else {
                i++;
            }
        }
        return i >= size() ? -1 : i;
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);  // nu duplicates are allowed
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        return new SkipListIteratorList();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return new SkipListIteratorList(index);
    }

    @NotNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {    // TODO: test
        if (fromIndex < 0 || fromIndex > toIndex || toIndex >= size()) {
            throw new IndexOutOfBoundsException();
        }
        var fromIndexKey = get(fromIndex);
        var toIndexKey = get(toIndex);
        if (fromIndexKey != null && toIndexKey != null) {
            return new SkipList<>() {{
                addAll(skipListMap.subMap(get(fromIndex), get(toIndex)).keySet());
            }};
        } else {
            throw new IllegalStateException("This should never happen (IndexOutofBound eventually already thrown).");
        }
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

    private class SkipListIteratorList implements ListIterator<T> {

        /**
         * The iterator of {@link SkipListMap}.
         */
        @NotNull
        private final Iterator<T> skipListIterator;
        /**
         * The currently pointed node.
         */
        @Nullable
        private final T current;
        /**
         * The previous node wrt. the currently pointed one.
         */
        @Nullable
        private T previous;
        /**
         * The index to the currently pointed node.
         */
        private int index;

        /**
         * Default constructor.
         */
        public SkipListIteratorList() {
            this.index = -1;// when call next, it is incremented (the first returned element will have index 0)
            this.current = null;
            this.previous = null;
            this.skipListIterator = skipListMap.iterator();
        }

        /**
         * Constructor. The first element returned by a call to {@link #next()} will
         * be the one corresponding to the index given as parameter.
         *
         * @param index The index of the element that is desired to be returned at the
         *              first invocation of {@link #next()}.
         */
        public SkipListIteratorList(int index) {    // TODO: test
            this();
            while (hasNext() && this.index == index) {
                next();
            }
        }

        @Override
        public boolean hasNext() {  // TODO: test
            return skipListIterator.hasNext();
        }

        @Override
        public T next() {  // TODO: test
            index++;
            previous = current;
            return skipListIterator.next();
        }

        @Override
        public boolean hasPrevious() {  // TODO: test
            return previous != null;
        }

        @Override
        public T previous() {  // TODO: test
            if (hasPrevious()) {
                return previous;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public int nextIndex() {  // TODO: test
            return index + 1;
        }

        @Override
        public int previousIndex() {  // TODO: test
            return index - 1;
        }

        @Override
        public void remove() {  // TODO: test
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(T t) {
            throw new UnsupportedOperationException();  // set must obey the order imposed by the data-structure
        }

        @Override
        public void add(T t) {
            throw new UnsupportedOperationException();  // add must obey the order imposed by the data-structure
        }
    }
}
