package moa.classifiers.lazy.ea;

import java.util.HashMap;
import java.util.Optional;

public class FitnessCache<K, V> {

    private HashMap<K, V> cache;

    public FitnessCache() {
        cache = new HashMap<>();
    }

    public void invalidate() {
        cache.clear();
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public Optional<V> get(K key) {
        return Optional.ofNullable(cache.get(key));
    }
}
