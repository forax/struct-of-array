package com.github.forax.soa;

import java.util.Arrays;
import java.util.Objects;

public final class StructOfArrayList$Person extends StructOfArrayList<Person> {
  private int[] array0;
  private String[] array1;

  StructOfArrayList$Person(int size, boolean unmodifiable, int[] array0, String[] array1) {
    super(size, unmodifiable);
    this.array0 = array0;
    this.array1 = array1;
  }

  StructOfArrayList$Person(int capacity, boolean unmodifiable) {
    this(0, unmodifiable, new int[capacity], new String[capacity]);
  }

  @Override
  final Person valueAt(int index) {
    return new Person(array0[index], array1[index]);
  }

  @Override
  final void valueAt(int index, Person element) {
    array0[index] = element.age();
    array1[index] = element.name();
  }

  @Override
  public Person get(int index) {
    Objects.checkIndex(index, size);
    return valueAt(index);
  }

  @Override
  public Person set(int index, Person element) {
    Objects.checkIndex(index, size);
    Objects.requireNonNull(element);
    var old = valueAt(index);
    valueAt(index, element);
    return old;
  }

  @Override
  public void add(int index, Person element) {
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException(index);
    }
    Objects.requireNonNull(element);
    if (size == array0.length) {
      resize();
    }
    System.arraycopy(array0, index, array0, index + 1, size - index);
    System.arraycopy(array1, index, array1, index + 1, size - index);
    valueAt(index, element);
    size++;
    modCount++;
  }

  @Override
  public final boolean contains(Object o) {
    return indexOf(o) != -1;
  }

  @Override
  public final boolean remove(Object o) {
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
    var index = indexOf(o);
    if (index == -1) {
      return false;
    }
    remove(index);
    return true;
  }

  @Override
  public Person remove(int index) {
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
    Objects.checkIndex(index, size);
    var old = valueAt(index);
    System.arraycopy(array0, index + 1, array0, index, size - index - 1);
    System.arraycopy(array1, index +1, array1, index, size - index - 1);
    var last = size - 1;
    //array0[size] = 0;
    array1[size] = null;
    size = last;
    modCount++;
    return old;
  }

  @Override
  public Person removeBySwappingLast(int index) {  // FIXME
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
    Objects.checkIndex(index, size);
    var old = valueAt(index);
    var last = size - 1;
    array0[index] = array0[last];
    array1[index] = array1[last];
    //array0[last] = 0;
    array1[last] = null;
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
      if (array0[i] == v0 && Objects.equals(array1[i], v1)) {
        return i;
      }
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
      if (array0[i] == v0 && Objects.equals(array1[i], v1)) {
        return i;
      }
    }
    return -1;
  }

  private void resize() {
    var size = this.size;
    var newSize = (size == 0)? 16: (int) (size * 1.5);
    array0 = Arrays.copyOf(array0, newSize);
    array1 = Arrays.copyOf(array1, newSize);
  }

  @Override
  public boolean add(Person element) {
    if (unmodifiable) {
      throw new UnsupportedOperationException();
    }
    Objects.requireNonNull(element);
    if (size == array0.length) {
      resize();
    }
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
    array0 = new int[0];
    array1 = new String[0];
    size = 0;
    modCount++;
  }
}
