package com.github.forax.soa;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.invoke.MethodType.methodType;

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

  public static <T extends Record> StructOfArrayList<T> of(Lookup lookup, Class<T> recordType) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordType);
    return of(lookup, recordType, 0);
  }

  public static <T extends Record> StructOfArrayList<T> of(Lookup lookup, Class<T> recordType, Collection<? extends T> collection) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordType);
    Objects.requireNonNull(collection);
    var soaList =  of(lookup, recordType, collection.size());
    soaList.addAll(collection);
    return soaList;
  }

  public static <T extends Record> StructOfArrayList<T> of(Lookup lookup, Class<T> recordType, int capacity) {
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
    var defaultConstructor = RT.defaultListConstructor(erasedLookup);
    try {
      return (StructOfArrayList<T>) defaultConstructor.invokeExact(capacity, false);
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable t) {
      throw (LinkageError) new LinkageError().initCause(t);
    }
  }
}
