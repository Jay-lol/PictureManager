package practice.kotlin.com.logoactivity2.mode.selected

import java.util.HashMap

class Cache<K, V> {
    private val mKVHashMap: HashMap<K, V>

    init {
        mKVHashMap = HashMap()
    }

    @Synchronized
    operator fun get(key: K): V? {
        return mKVHashMap[key]
    }

    @Synchronized
    fun put(key: K, value: V) {
        mKVHashMap[key] = value
    }
}