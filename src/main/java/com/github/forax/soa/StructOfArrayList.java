package com.github.forax.soa;

import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public abstract class StructOfArrayList<T> extends AbstractList<T> {
  int size;
  final boolean unmodifiable;

  StructOfArrayList(int size, boolean unmodifiable) {
    this.size = size;
    this.unmodifiable = unmodifiable;
  }

  public final int size() {
    return size;
  }

  @Override
  public final boolean isEmpty() {
    return size == 0;
  }

  abstract T valueAt(int index);
  abstract void valueAt(int index, T element);

  @Override
  public final T get(int index) {
    Objects.checkIndex(index, size);
    return valueAt(index);
  }

  @Override
  public final T set(int index, T element) {
    Objects.checkIndex(index, size);
    Objects.requireNonNull(element);
    var old = valueAt(index);
    valueAt(index, element);
    return old;
  }

  @Override
  public final void add(int index, T element) {
    throw new UnsupportedOperationException();
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
  public final Iterator<T> iterator() {
    var currentCount = modCount;
    return new Iterator<T>() {
      private int index;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public T next() {
        if (currentCount != modCount) {
          throw new ConcurrentModificationException();
        }
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return valueAt(index++);
      }
    };
  }

  @Override
  public final ListIterator<T> listIterator() {
    return listIterator(0);
  }

  @Override
  public final ListIterator<T> listIterator(int start) {
    if (start < 0 || start > size) {
      throw new IndexOutOfBoundsException();
    }
    var currentCount = modCount;
    return new ListIterator<T>() {
      private int index = start;
      private int last = -1;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public T next() {
        if (currentCount != modCount) {
          throw new ConcurrentModificationException();
        }
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return valueAt(last = index++);
      }

      @Override
      public boolean hasPrevious() {
        return index != 0;
      }

      @Override
      public T previous() {
        if (currentCount != modCount) {
          throw new ConcurrentModificationException();
        }
        if (!hasPrevious()) {
          throw new NoSuchElementException();
        }
        return valueAt(last = --index);
      }

      @Override
      public int nextIndex() {
        return index;
      }

      @Override
      public int previousIndex() {
        return index - 1;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void set(T element) {
        Objects.requireNonNull(element);
        if (last == -1) {
          throw new IllegalStateException();
        }
        valueAt(last, element);
        last = -1;
      }

      @Override
      public void add(T t) {
        throw new UnsupportedOperationException();
      }
    };
  }

  public static <T extends Record> StructOfArrayList<T> of(Class<T> recordType) {
    Objects.requireNonNull(recordType);
    return of(recordType, 0);
  }

  public static <T extends Record> StructOfArrayList<T> of(Class<T> recordType, Collection<? extends T> collection) {
    Objects.requireNonNull(recordType);
    Objects.requireNonNull(collection);
    var soaList =  of(recordType, collection.size());
    soaList.addAll(collection);
    return soaList;
  }

  public static <T extends Record> StructOfArrayList<T> of(Class<T> recordType, int capacity) {
    Objects.requireNonNull(recordType);
    if (capacity < 0) {
      throw new IllegalArgumentException("capacity < 0");
    }
    if (recordType != Person.class) {
      throw new UnsupportedOperationException("NYI");
    }
    return (StructOfArrayList<T>)(StructOfArrayList<?>) new StructOfArrayList$Template(capacity, false);
  }
}
