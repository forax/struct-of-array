package com.github.forax.soa;

import java.lang.invoke.MethodHandles.Lookup;
import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A list of record item that stores each component of the record inside its own array.
 * For example, with a record
 * <pre>
 *   record Point(int x, int y) {}
 * </pre>
 * this list uses two int arrays, one for the component x and one for the component y.
 * <p>
 * This data structure is more compact that the usual {@code ArrayList} and is suitable
 * if you are trying to fit everything inside the L2 cache (if you have no more than a thousand values).
 * <p>
 * Because the content of a record is stored instead of storing the object itself,
 * the identity of the record are no preserved, storing a record and retrieving it
 * are equals in the sense of {@link Object#equals(Object)} but not in the sense or {@code ==}.
 * <p>
 * This implementation support empty records, in that case, the list will only count the number
 * of items inserted without storing them.
 * <p>
 * The implantations of {@link #remove(int)} and {@link #remove(Object)} are unconventional,
 * instead of shifting all the elements that are after the one removed, the implementations
 * remove the last element and place it at the location of the removed element which it
 * more efficient. So calling {@code remove} does not keep the insertion order.
 * <p>
 * To avoid the usual performance issue of {@link #add(int, Object)}, this method is not implemented,
 * even if the index is just after the last element, use {@link #add(Object)} instead.
 * <p>
 * Structural modification are not allowed during an iteration, even using {@link Iterator#remove()}
 * or {@link ListIterator#add(Object)} again because otherwise performance would not be great.
 * <p>
 * Null as an element is not supported (it's not a record after all) so all methods that takes
 * an element as parameter throw a {@link NullPointerException} if {@code null} is passed.
 * <p>
 * If you know the approximative size of the list, consider using {@link #of(Lookup, Class, int)}
 * with the capacity as last parameter.
 *
 * @param <E> the type of the item, must be a record
 *
 * @see StructOfArrayMap
 */
public abstract class StructOfArrayList<E> extends AbstractList<E> {
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

  abstract E valueAt(int index);
  abstract void valueAt(int index, E element);

  @Override
  public final E get(int index) {
    Objects.checkIndex(index, size);
    return valueAt(index);
  }

  @Override
  public final E set(int index, E element) {
    Objects.checkIndex(index, size);
    Objects.requireNonNull(element);
    var old = valueAt(index);
    valueAt(index, element);
    return old;
  }

  @Override
  public final void add(int index, E element) {
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
  public final Iterator<E> iterator() {
    var currentCount = modCount;
    return new Iterator<E>() {
      private int index;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public E next() {
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
  public final ListIterator<E> listIterator() {
    return listIterator(0);
  }

  @Override
  public final ListIterator<E> listIterator(int start) {
    if (start < 0 || start > size) {
      throw new IndexOutOfBoundsException();
    }
    var currentCount = modCount;
    return new ListIterator<E>() {
      private int index = start;
      private int last = -1;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public E next() {
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
      public E previous() {
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
      public void set(E element) {
        Objects.requireNonNull(element);
        if (last == -1) {
          throw new IllegalStateException();
        }
        valueAt(last, element);
        last = -1;
      }

      @Override
      public void add(E e) {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Creates a struct of arrays seen as a list.
   *
   * @param lookup a lookup that can access to the record
   * @param recordType a record class
   * @return a fresh empty list
   * @throws NullPointerException if one of the parameter is null
   * @throws IllegalArgumentException if the recordType is not a record
   * @throws IllegalStateException if the lookup can not access to the record class
   * @param <T> the type of the list item
   */
  public static <T extends Record> StructOfArrayList<T> of(Lookup lookup, Class<T> recordType) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordType);
    return of(lookup, recordType, 0);
  }

  /**
   * Creates a struct of arrays seen as a list with a collection of items to initialize the list.
   *
   * @param lookup a lookup that can access to the record
   * @param recordType a record class
   * @param collection a collection of items
   * @return a fresh empty list
   * @throws NullPointerException if one of the parameter is null
   * @throws IllegalArgumentException if the recordType is not a record
   * @throws IllegalStateException if the lookup can not access to the record class
   * @param <T> the type of the list item
   */
  public static <T extends Record> StructOfArrayList<T> of(Lookup lookup, Class<T> recordType, Collection<? extends T> collection) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordType);
    Objects.requireNonNull(collection);
    var soaList =  of(lookup, recordType, collection.size());
    soaList.addAll(collection);
    return soaList;
  }

  /**
   * Creates a struct of arrays seen as a list with a given initial capacity.
   *
   * @param lookup a lookup that can access to the record
   * @param recordType a record class
   * @param capacity an initial capacity
   * @return a fresh empty list
   * @throws NullPointerException if one of the parameter is null
   * @throws IllegalArgumentException if the recordType is not a record or the capacity is negative
   * @throws IllegalStateException if the lookup can not access to the record class
   * @param <T> the type of the list item
   */
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
