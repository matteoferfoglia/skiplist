package skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class extending the {@link SkipListMap} which exploits an HashTable
 * to access by key in time O(1) the values of the {@link SkipListMap}
 * (which is still used).
 * We should note that using this data-structure requires much more
 * memory space, because, additionally to the base {@link SkipListMap},
 * another HashMap is saved to achieve the O(1) by-key access time (the
 * latter contains the same entries!).
 *
 * @author Matteo Ferfoglia
 */
public class SkipListHashMap<K extends Comparable<K>, V> extends SkipListMap<K, V> implements Externalizable {

    /**
     * {@link ConcurrentHashMap} which works together with the {@link SkipListMap}.
     * In this way it is possible to keep all the capabilities of the {@link SkipListMap},
     * and, additionally, it is possible to access a value of the map in time O(1),
     * thanks to this HashMap.
     */
    private final HashMap<K, V> hashMapToValues = new HashMap<>();

    @Override
    public synchronized boolean containsKey(Object key) {
        return hashMapToValues.containsKey(key);
    }

    @Override
    @Nullable
    public synchronized V get(Object key) {
        return hashMapToValues.get(key);
    }

    @Override
    @Nullable
    public synchronized V put(@NotNull K key, V value) {
        V v1 = hashMapToValues.put(key, value);
        V v2 = super.put(key, value);
        assert Objects.equals(v1, v2);
        return v2;
    }

    @Override
    @Nullable
    public synchronized V remove(@NotNull Object key) {
        V v1 = hashMapToValues.remove(key);
        V v2 = super.remove(key);
        assert Objects.equals(v1, v2);
        return v2;
    }

    @Override
    @NotNull SkipListNode<K, V> copyNodeAndInsertAtEnd(@NotNull SkipListNode<K, V> node) {
        var copiedNode = super.copyNodeAndInsertAtEnd(node);
        assert copiedNode.getKey() != null;
        assert copiedNode.getValue() != null;
        hashMapToValues.put(copiedNode.getKey(), copiedNode.getValue());
        return copiedNode;
    }

    @Override
    public synchronized void clear() {
        hashMapToValues.clear();
        super.clear();
    }

    @Override
    public synchronized void putAll(@NotNull Map<? extends K, ? extends V> m) {
        hashMapToValues.putAll(m);
        super.putAll(m);
    }

    @Override
    public synchronized boolean putAllKeys(@NotNull Collection<? extends K> keys) {
        putAllKeysInPrivateHashMap(keys);
        return super.putAllKeys(keys);
    }

    /**
     * Utility function used to put all keys in {@link #hashMapToValues}.
     *
     * @param keys The keys to add to {@link #hashMapToValues}. Corresponding values
     *             will be null.
     */
    private void putAllKeysInPrivateHashMap(@NotNull Collection<? extends K> keys) {
        hashMapToValues.putAll(keys.stream().collect(
                HashMap::new, (m, v) -> m.put(v, null), HashMap::putAll));
    }

    @Override
    public synchronized boolean putAllKeysWithoutCheckingOrderAtTheEnd(@NotNull Collection<? extends K> keys) {
        putAllKeysInPrivateHashMap(keys);
        return super.putAllKeysWithoutCheckingOrderAtTheEnd(keys);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SkipListHashMap<?, ?> that = (SkipListHashMap<?, ?>) o;

        return hashMapToValues.equals(that.hashMapToValues);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + hashMapToValues.hashCode();
        return result;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(hashMapToValues);
        super.writeExternal(out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        hashMapToValues.clear();
        //noinspection unchecked    // casting for correct deserialization
        hashMapToValues.putAll((HashMap<K, V>) in.readObject());
        super.readExternal(in);
    }

    /**
     * @return a <strong>copy</strong>copy the {@link HashMap} used by this object.
     */
    public HashMap<K, V> getCopyOfHashMap() {
        return new HashMap<>(hashMapToValues);
    }
}