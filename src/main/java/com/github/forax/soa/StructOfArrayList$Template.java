package com.github.forax.soa;

import java.util.Arrays;
import java.util.Objects;

public final class StructOfArrayList$Template extends StructOfArrayList<Person> {
  private int[] array0;
  private String[] array1;

  StructOfArrayList$Template(int size, boolean unmodifiable, int[] array0, String[] array1) {
    super(size, unmodifiable);
    Snippets.start();
    this.array0 = array0;
    this.array1 = array1;
    Snippets.end();
  }

  StructOfArrayList$Template(int capacity, boolean unmodifiable) {
    this(0, unmodifiable, new int[capacity], new String[capacity]);
  }

  @Override
  final Person valueAt(int index) {
    Snippets.start();
    var element = new Person(array0[index], array1[index]);
    Snippets.end();
    return element;
  }

  @Override
  final void valueAt(int index, Person element) {
    Snippets.start();
    array0[index] = element.age();
    array1[index] = element.name();
    Snippets.end();
  }

  public Person remove(int index) {
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
    Objects.checkIndex(index, size);
    var old = valueAt(index);
    var last = size - 1;
    Snippets.start();
    array0[index] = array0[last];
    array1[index] = array1[last];
    Snippets.end();
    Snippets.start();
    //array0[last] = 0;
    array1[last] = null;
    Snippets.end();
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
    var v0 = element.age();
    var v1 = element.name();
    for(var i = 0; i < size; i++) {
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
    var v0 = person.age();
    var v1 = person.name();
    for(var i = size; --i >= 0;) {
      Snippets.start();
      if (array0[i] == v0 && Objects.equals(array1[i], v1)) {
        return i;
      }
      Snippets.end();
    }
    return -1;
  }

  private void resize() {
    var size = this.size;
    var newLength = (size == 0)? 16: (int) (size * 1.5);
    Snippets.start();
    array0 = Arrays.copyOf(array0, newLength);
    array1 = Arrays.copyOf(array1, newLength);
    Snippets.end();
  }

  @Override
  public boolean add(Person element) {
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
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
