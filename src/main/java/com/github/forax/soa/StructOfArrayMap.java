package com.github.forax.soa;

import java.lang.invoke.MethodHandles.Lookup;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A hash map that stores integer keys and record values inside a struct of arrays.
 * <p>
 * This representation is more compact than using a {@link java.util.HashMap} but
 * if less efficient once the threshold of few thousand items is crossed.
 * <p>
 * The {@link #values()} are viewed as an {@link StructOfArrayList} so are kept in the insertion order
 * apart if {@link #remove(Object)} is called. This view does not allow structural modification so
 * {@link StructOfArrayList#add(Object)}, {@link StructOfArrayList#remove(int)} and
 * {@link StructOfArrayList#remove(Object)} are not supported.
 * <p>
 * Iterating over the keys or the values is very efficient (if everything is kept in the L2 cache)
 * because the iteration is not done on the hash table itself.
 * <p>
 * Structural modification are not allowed during an iteration, so {@link Iterator#remove()}
 * is not implemented on {@link #keySet()}, {@link #entrySet()} or {@link #values()}.
 * <p>
 * Null as a value is not supported (it's not a record after all) so all methods that takes
 * a key or a value as parameter throw a {@link NullPointerException} if {@code null} is passed.
 * <p>
 * If you know the approximative size of the list, consider using {@link #of(Lookup, Class, int)}
 * with the capacity as last parameter.
 *
 * @param <E> the type of the item
 *
 * @see StructOfArrayList
 */
public abstract class StructOfArrayMap<E> extends AbstractMap<Integer, E> {
  static final int EMPTY = -1;
  static final int TOMBSTONE = -2;

  int size;

  int[] indexes;
  int[] keys;

  int modCount;

  StructOfArrayMap(int capacity) {
    indexes = new int[capacity << 1];
    Arrays.fill(indexes, EMPTY);
    keys = new int[capacity];
  }

  private static void insert(int[] newIndexes, int k, int newIndex) {
    var slot = k & (newIndexes.length - 1);
    for(;;) {
      var index = newIndexes[slot];
      if (index == EMPTY) {
        newIndexes[slot] = newIndex;
        return;
      }
      slot = (slot + 1) & (newIndexes.length - 1);
    }
  }

  final int[] rehash() {
    var newIndexes = new int[indexes.length << 1];
    Arrays.fill(newIndexes, EMPTY);
    for (int index : indexes) {
      if (index < 0) {  // EMPTY or TOMBSTONE
        continue;
      }
      insert(newIndexes, keys[index], index);
    }
    return newIndexes;
  }

  @Override
  public final int size() {
    return size;
  }

  @Override
  public final boolean isEmpty() {
    return size == 0;
  }

  @Override
  public final boolean containsKey(Object key) {
    Objects.requireNonNull(key);
    if (!(key instanceof Integer value)) {
      return false;
    }
    var k = (int) value;
    var slot = k & (indexes.length - 1);
    for(;;) {
      var index = indexes[slot];
      if (index == EMPTY) {
        return false;
      }
      if (index != TOMBSTONE && keys[index] == k) {
        return true;
      }
      slot = (slot + 1) & (indexes.length - 1);
    }
  }

  @Override
  public final E get(Object key) {
    return getOrDefault(key, null);
  }

  @Override
  public final E getOrDefault(Object key, E defaultValue) {
    Objects.requireNonNull(key);
    if (!(key instanceof Integer value)) {
      return defaultValue;
    }
    var k = (int) value;
    var slot = k & (indexes.length - 1);
    for(;;) {
      var index = indexes[slot];
      if (index == EMPTY) {
        return defaultValue;
      }
      if (index != TOMBSTONE && keys[index] == k) {
        return valueAt(index);
      }
      slot = (slot + 1) & (indexes.length - 1);
    }
  }

  abstract void resize();

  @Override
  public final E put(Integer key, E value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    var k = (int) key;
    var indexes = this.indexes;
    var slot = k & (indexes.length - 1);
    for(;;) {
      var index = indexes[slot];
      if (index < 0) {  // EMPTY or TOMBSTONE
        if (size == keys.length) {
          resize();
          indexes = this.indexes;
          slot = k & (indexes.length - 1);
          continue;
        }
        var newIndex = size++;
        indexes[slot] = newIndex;
        keys[newIndex] = k;
        valueAt(newIndex, value);
        modCount++;
        return null;
      }
      if (keys[index] == k) {
        var old = valueAt(index);
        valueAt(index, value);
        return old;
      }
      slot = (slot + 1) & (indexes.length - 1);
    }
  }

  void replaceLastKeyIndex(int k, int lastIndex, int newIndex) {
    var slot = k & (indexes.length - 1);
    for(;;) {
      var index = indexes[slot];
      assert index != EMPTY;
      if (index != TOMBSTONE && index == lastIndex) {
        indexes[slot] = newIndex;
        return;
      }
      slot = (slot + 1) & (indexes.length - 1);
    }
  }

  @Override
  public final E replace(Integer key, E value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    var k = (int) key;
    var slot = k & (indexes.length - 1);
    for(;;) {
      var index = indexes[slot];
      if (index == EMPTY) {
        return null;
      }
      if (index != TOMBSTONE && keys[index] == k) {
        var old = valueAt(index);
        valueAt(index, value);
        return old;
      }
      slot = (slot + 1) & (indexes.length - 1);
    }
  }

  abstract E valueAt(int index);
  abstract void valueAt(int index, E element);

  @Override
  public final Set<Entry<Integer, E>> entrySet() {
    return new AbstractSet<>() {
      @Override
      public int size() {
        return size;
      }

      @Override
      public boolean remove(Object o) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Iterator<Entry<Integer, E>> iterator() {
        var keys = StructOfArrayMap.this.keys;
        var currentCount = modCount;
        return new Iterator<>() {
          private int index;

          @Override
          public boolean hasNext() {
            return index < size;
          }

          @Override
          public Entry<Integer, E> next() {
            if (modCount != currentCount) {
              throw new ConcurrentModificationException();
            }
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            var index = this.index;
            var key = keys[index];
            this.index = index + 1;
            return new Entry<>() {
              @Override
              public boolean equals(Object obj) {
                return obj instanceof Map.Entry<?,?> entry &&
                    ((Integer)key).equals(entry.getKey()) &&
                    valueAt(index).equals(entry.getValue());
              }

              @Override
              public int hashCode() {
                return Integer.hashCode(key) ^ valueAt(index).hashCode();
              }

              @Override
              public String toString() {
                return key + "=" + valueAt(index);
              }

              @Override
              public Integer getKey() {
                return key;
              }

              @Override
              public E getValue() {
                return valueAt(index);
              }

              @Override
              public E setValue(E value) {
                var old = valueAt(index);
                valueAt(index, value);
                return old;
              }
            };
          }
        };
      }
    };
  }

  @Override
  public final Set<Integer> keySet() {
    return new AbstractSet<>() {
      @Override
      public int size() {
        return size;
      }

      @Override
      public boolean contains(Object o) {
        return containsKey(o);
      }

      @Override
      public boolean remove(Object o) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Iterator<Integer> iterator() {
        var keys = StructOfArrayMap.this.keys;
        var currentCount = modCount;
        return new PrimitiveIterator.OfInt() {
          private int index;

          @Override
          public boolean hasNext() {
            return index < size;
          }

          @Override
          public int nextInt() {
            if (modCount != currentCount) {
              throw new ConcurrentModificationException();
            }
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return keys[index++];
          }
        };
      }
    };
  }

  @Override
  public abstract StructOfArrayList<E> values();

  @Override
  public void forEach(BiConsumer<? super Integer, ? super E> action) {
    Objects.requireNonNull(action);
    for (var i = 0; i < size; i++) {
      action.accept(keys[i], valueAt(i));
    }
  }

  /**
   * Create an empty map.
   *
   * @param lookup a lookup that can access to the record
   * @param recordType a record class
   * @return a fresh empty map
   * @throws NullPointerException if one of the parameter is null
   * @throws IllegalArgumentException if the recordType is not a record
   * @throws IllegalStateException if the lookup can not access to the record class
   * @param <T> the type of the map value
   */
  public static <T extends Record> StructOfArrayMap<T> of(Lookup lookup, Class<T> recordType) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordType);
    return of(lookup, recordType, 0);
  }

  /**
   * Create a map populated with the keys and valus of an existing map.
   *
   * @param lookup a lookup that can access to the record
   * @param recordType a record class
   * @param map an existing map
   * @return a newly created map populated with the keys and valus of an existing map.
   * @throws NullPointerException if one of the parameter is null
   * @throws IllegalArgumentException if the recordType is not a record
   * @throws IllegalStateException if the lookup can not access to the record class
   * @param <T> the type of the map value
   */
  public static <T extends Record> StructOfArrayMap<T> of(Lookup lookup, Class<T> recordType, Map<? extends Integer, ? extends T> map) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordType);
    Objects.requireNonNull(map);
    var soaMap =  of(lookup, recordType, map.size());
    soaMap.putAll(map);
    return soaMap;
  }

  /**
   * Create an empty map with an initial capacity.
   *
   * @param lookup a lookup that can access to the record
   * @param recordType a record class
   * @param capacity an initial capacity
   * @return a fresh empty map
   * @throws NullPointerException if one of the parameter is null
   * @throws IllegalArgumentException if the recordType is not a record or the capacity is negative
   * @throws IllegalStateException if the lookup can not access to the record class
   * @param <T> the type of the map value
   */
  public static <T extends Record> StructOfArrayMap<T> of(Lookup lookup, Class<T> recordType, int capacity) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordType);
    try {
      lookup.accessClass(recordType);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
    if (!recordType.isRecord()) {
      throw new IllegalArgumentException("recordType is not a record");
    }
    if (capacity < 0) {
      throw new IllegalArgumentException("capacity < 0");
    }
    var erasedLookup = lookup.in(recordType);
    var defaultConstructor = RT.defaultMapConstructor(erasedLookup);
    var powerOf2 = Math.max(16, (capacity & (capacity - 1)) == 0? capacity: Integer.highestOneBit(capacity) << 1);
    try {
      return (StructOfArrayMap<T>) defaultConstructor.invokeExact(powerOf2);
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable t) {
      throw (LinkageError) new LinkageError().initCause(t);
    }
  }
}
