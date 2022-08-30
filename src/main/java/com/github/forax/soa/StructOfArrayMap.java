package com.github.forax.soa;

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

public abstract class StructOfArrayMap<T> extends AbstractMap<Integer, T> {
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
  public final T get(Object key) {
    return getOrDefault(key, null);
  }

  @Override
  public final T getOrDefault(Object key, T defaultValue) {
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
  public final T put(Integer key, T value) {
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

  final void replaceLastKeyIndex(int k, int lastIndex, int newIndex) {
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
  public final T replace(Integer key, T value) {
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

  abstract T valueAt(int index);
  abstract void valueAt(int index, T element);

  @Override
  public final Set<Entry<Integer, T>> entrySet() {
    return new AbstractSet<>() {
      @Override
      public int size() {
        return size;
      }

      @Override
      public Iterator<Entry<Integer, T>> iterator() {
        var keys = StructOfArrayMap.this.keys;
        var currentCount = modCount;
        return new Iterator<>() {
          private int index;

          @Override
          public boolean hasNext() {
            return index < size;
          }

          @Override
          public Entry<Integer, T> next() {
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
              public T getValue() {
                return valueAt(index);
              }

              @Override
              public T setValue(T value) {
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
  public abstract StructOfArrayList<T> values();

  @Override
  public void forEach(BiConsumer<? super Integer, ? super T> action) {
    Objects.requireNonNull(action);
    for (int index : indexes) {
      if (index < 0) {  // EMPTY or TOMBSTONE
        continue;
      }
      action.accept(keys[index], valueAt(index));
    }
  }

  public static <T extends Record> StructOfArrayMap<T> of(Class<T> recordType) {
    Objects.requireNonNull(recordType);
    return of(recordType, 0);
  }

  public static <T extends Record> StructOfArrayMap<T> of(Class<T> recordType, Map<? extends Integer, ? extends T> map) {
    Objects.requireNonNull(recordType);
    Objects.requireNonNull(map);
    var soaMap =  of(recordType, map.size());
    soaMap.putAll(map);
    return soaMap;
  }

  public static <T extends Record> StructOfArrayMap<T> of(Class<T> recordType, int capacity) {
    Objects.requireNonNull(recordType);
    if (capacity < 0) {
      throw new IllegalArgumentException("capacity < 0");
    }
    if (recordType != Person.class) {
      throw new UnsupportedOperationException("NYI");
    }
    var powerOf2 = Math.max(16, (capacity & (capacity - 1)) == 0? capacity: Integer.highestOneBit(capacity) << 1);
    return (StructOfArrayMap<T>)(StructOfArrayMap<?>) new StructOfArrayMap$Template(powerOf2);
  }
}
