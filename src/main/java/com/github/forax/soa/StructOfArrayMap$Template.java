package com.github.forax.soa;

import java.util.Arrays;
import java.util.Objects;

public final class StructOfArrayMap$Template extends StructOfArrayMap {
  private int[] array0;
  private String[] array1;

  public StructOfArrayMap$Template(int capacity) {
    super(capacity);
    Snippets.start();
    array0 = new int[capacity];
    array1 = new String[capacity];
    Snippets.end();
  }

  @Override
  final Object valueAt(int index) {
    Snippets.start();
    var element = new Person(array0[index], array1[index]);
    Snippets.end();
    return element;
  }

  @Override
  final void valueAt(int index, Object item) {
    var value = (Person) item;
    Snippets.start();
    array0[index] = value.age();
    array1[index] = value.name();
    Snippets.end();
  }

  @Override
  public boolean containsValue(Object value) {
    Objects.requireNonNull(value);
    if (!(value instanceof Person element)) {
      return false;
    }
    int i;  // must be declared before the snippet
    Snippets.start();
    var v0 = element.age();
    var v1 = element.name();
    Snippets.end();
    for(i = 0; i < size; i++) {
      Snippets.start();
      if (array0[i] == v0 && Objects.equals(array1[i], v1)) {
        return true;
      }
      Snippets.end();
    }
    return false;
  }

  @Override
  final void resize() {
    indexes = rehash();
    var newLength = size << 1;
    keys = Arrays.copyOf(keys, newLength);
    Snippets.start();
    array0 = Arrays.copyOf(array0, newLength);
    array1 = Arrays.copyOf(array1, newLength);
    Snippets.end();
  }

  @Override
  public void clear() {
    indexes = new int[32];
    keys = new int[16];
    Snippets.start();
    array0 = new int[16];
    array1 = new String[16];
    Snippets.end();
    size = 0;
    modCount++;
  }

  @Override
  public StructOfArrayList values() {
    Snippets.start();
    var values = new StructOfArrayList$Template(size, true, array0, array1);
    Snippets.end();
    return values;
  }

  @Override
  public Object remove(Object key) {
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
          Snippets.start();
          array0[index] = array0[last];
          array1[index] = array1[last];
          Snippets.end();
        }
        //keys[last] = 0;
        Snippets.start();
        //array0[last] = 0;
        array1[last] = null;
        Snippets.end();
        size--;
        modCount++;
        return old;
      }
      slot = (slot + 1) & (indexes.length - 1);
    }
  }
}
