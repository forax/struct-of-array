package com.github.forax.soa;

import java.util.Arrays;
import java.util.Objects;

public final class StructOfArrayMap$Person extends StructOfArrayMap<Person> {
  private int[] array0;
  private String[] array1;

  StructOfArrayMap$Person(int capacity) {
    super(capacity);
    array0 = new int[capacity];
    array1 = new String[capacity];
  }

  @Override
  final Person valueAt(int index) {
    return new Person(array0[index], array1[index]);
  }

  @Override
  final void valueAt(int index, Person value) {
    array0[index] = value.age();
    array1[index] = value.name();
  }

  @Override
  public boolean containsValue(Object value) {
    Objects.requireNonNull(value);
    if (!(value instanceof Person element)) {
      return false;
    }
    var v0 = element.age();
    var v1 = element.name();
    for(var i = 0; i < size; i++) {
      if (array0[i] == v0 && Objects.equals(array1[i], v1)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Person get(Object key) {
    return getOrDefault(key, null);
  }

  @Override
  public Person getOrDefault(Object key, Person defaultValue) {
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

  private void resize() {
    indexes = rehash();
    var newSize = size << 1;
    keys = Arrays.copyOf(keys, newSize);
    array0 = Arrays.copyOf(array0, newSize);
    array1 = Arrays.copyOf(array1, newSize);
  }

  @Override
  public Person put(Integer key, Person value) {
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

  @Override
  public void clear() {
    indexes = new int[32];
    keys = new int[16];
    array0 = new int[16];
    array1 = new String[16];
    size = 0;
    modCount++;
  }

  @Override
  public StructOfArrayList<Person> values() {
    return new StructOfArrayList$Person(size, true, array0, array1);
  }

  private void replaceLastKeyIndex(int k, int lastIndex, int newIndex) {
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
  public Person remove(Object key) {
    Objects.requireNonNull(key);
    if (!(key instanceof Integer integer)) {
      return null;
    }
    var k = (int) integer;
    var slot = k & (indexes.length - 1);
    for(;;) {
      var index = indexes[slot];
      if (index == EMPTY) {
        return null;
      }
      if (index != TOMBSTONE && keys[index] == k) {
        var old = valueAt(index);
        indexes[slot] = TOMBSTONE;
        var last = size - 1;
        if (index != last) {
          var lastKey = keys[last];
          replaceLastKeyIndex(lastKey, last, index);
          keys[index] = lastKey;
          array0[index] = array0[last];
          array1[index] = array1[last];
        }
        //keys[last] = 0;
        //array0[last] = 0;
        array1[last] = null;
        size--;
        modCount++;
        return old;
      }
      slot = (slot + 1) & (indexes.length - 1);
    }
  }

  @Override
  public Person replace(Integer key, Person value) {
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
}
