package com.github.forax.soa;

import java.util.Arrays;
import java.util.Objects;

public final class StructOfArrayList$Template extends StructOfArrayList {
  private int[] array0;
  private String[] array1;

  /* snippet */
  public StructOfArrayList$Template(int size, boolean unmodifiable, int[] array0, String[] array1) {
    super(size, unmodifiable);
    this.array0 = array0;
    this.array1 = array1;
  }

  /* snippet */
  public StructOfArrayList$Template(int capacity, boolean unmodifiable) {
    this(0, unmodifiable, new int[capacity], new String[capacity]);
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
    var element = (Person) item;
    Snippets.start();
    array0[index] = element.age();
    array1[index] = element.name();
    Snippets.end();
  }

  private void copyElement(int to, int from) {
    Snippets.start();
    array0[to] = array0[from];
    array1[to] = array1[from];
    Snippets.end();
  }

  private void zeroElement(int index) {
    Snippets.start();
    //array0[index] = 0;
    array1[index] = null;
    Snippets.end();
  }

  public Object remove(int index) {
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
    Objects.checkIndex(index, size);
    var old = valueAt(index);
    var last = size - 1;
    copyElement(index, last);
    zeroElement(last);
    size = last;
    modCount++;
    return old;
  }

  @Override
  public int indexOf(Object o) {
    Objects.requireNonNull(o);
    if (!(o instanceof Person element)) {
      return -1;
    }
    int i;  // must be declared before the snippet
    Snippets.start();
    var v0 = element.age();
    var v1 = element.name();
    Snippets.end();
    for(i = 0; i < size; i++) {
      Snippets.start();
      if (array0[i] == v0 && Objects.equals(array1[i], v1)) {
        return i;
      }
      Snippets.end();
    }
    return -1;
  }

  @Override
  public int lastIndexOf(Object o) {
    Objects.requireNonNull(o);
    if (!(o instanceof Person person)) {
      return -1;
    }
    int i;  // must be declared before the snippet
    Snippets.start();
    var v0 = person.age();
    var v1 = person.name();
    Snippets.end();
    for(i = size; --i >= 0;) {
      Snippets.start();
      if (array0[i] == v0 && Objects.equals(array1[i], v1)) {
        return i;
      }
      Snippets.end();
    }
    return -1;
  }

  private void copyAll(int newLength) {
    Snippets.start();
    array0 = Arrays.copyOf(array0, newLength);
    array1 = Arrays.copyOf(array1, newLength);
    Snippets.end();
  }

  private void resize() {
    var size = this.size;
    var newLength = (size == 0)? 16: (int) (size * 1.5);
    copyAll(newLength);
  }

  @Override
  public boolean add(Object item) {
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
    var element = (Person) item;
    Objects.requireNonNull(element);
    Snippets.start();
    if (size == array0.length) {
      resize();
    }
    Snippets.end();
    var index = size;
    valueAt(index, element);
    size = index + 1;
    modCount++;
    return true;
  }

  @Override
  public void clear() {
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
    Snippets.start();
    array0 = new int[0];
    array1 = new String[0];
    Snippets.end();
    size = 0;
    modCount++;
  }
}
