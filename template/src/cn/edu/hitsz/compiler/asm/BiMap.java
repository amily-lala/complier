package cn.edu.hitsz.compiler.asm;

import java.util.HashMap;
import java.util.Map;

/**
 * 双向映射map实现
 * @param <K> key
 * @param <V> value
 * 实现思路：
 *           创建两个hashMap：Map<K,V> Kmap  Map<V,K> Vmap
 *           重写方法 get put containsKey remove
 *           检查
 */
public class BiMap<K,V> {

    private final Map<K,V> Kmap = new HashMap<>();
    private final Map<V,K> Vmap = new HashMap<>();

    public V getByK (K key) {
        return Kmap.get(key);
    }

    public K getByV (V value) {
        return Vmap.get(value);
    }

    public void removeByK (K key) {
        Kmap.remove(key);
    }

    public void removeByV (V value) {
        Vmap.remove(value);
    }

    public void put (K key,V value) {
        // 可能出现交叉项
        Kmap.remove(key);
        Vmap.remove(value);

        Kmap.put(key,value);
        Vmap.put(value,key);
    }

    public boolean containsKey (K key) {
        return Kmap.containsKey(key);
    }

    public boolean containsValue (V value) {
        return Vmap.containsKey(value);
    }
}
