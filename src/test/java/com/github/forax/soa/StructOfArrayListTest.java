package com.github.forax.soa;

import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class StructOfArrayListTest {
  @Test
  public void of() {
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> StructOfArrayList.of(null)),
        () -> assertThrows(NullPointerException.class, () -> StructOfArrayList.of(Person.class, null)),
        () -> assertThrows(NullPointerException.class, () -> StructOfArrayList.of(null, List.of())),
        () -> assertThrows(NullPointerException.class, () -> StructOfArrayList.of(null, 8)),
        () -> assertThrows(IllegalArgumentException.class, () -> StructOfArrayList.of(Person.class, -1))
    );
  }

  @Test
  public void sizeAndIsEmpty() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(18, "Bob"));
    assertAll(
        () -> assertFalse(soaList.isEmpty()),
        () -> assertEquals(2, soaList.size()),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(18, "Bob")), soaList)
    );
  }

  @Test
  public void sizeAndIsEmptyIfEmpty() {
    var soaList = StructOfArrayList.of(Person.class);
    assertAll(
        () -> assertTrue(soaList.isEmpty()),
        () -> assertEquals(0, soaList.size()),
        () -> assertEquals(List.of(), soaList)
    );
  }

  @Test
  public void addAndGet() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(18, "Bob"));
    assertAll(
        () -> assertEquals(36, soaList.get(0).age()),
        () -> assertEquals("Ana", soaList.get(0).name()),
        () -> assertEquals(18, soaList.get(1).age()),
        () -> assertEquals("Bob", soaList.get(1).name()),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(18, "Bob")), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(2))
    );
  }

  @Test
  public void addPopulatedAndGet() {
    var soaList = StructOfArrayList.of(Person.class, List.of(
        new Person(36, "Ana"),
        new Person(18, "Bob")
    ));
    assertAll(
        () -> assertEquals(36, soaList.get(0).age()),
        () -> assertEquals("Ana", soaList.get(0).name()),
        () -> assertEquals(18, soaList.get(1).age()),
        () -> assertEquals("Bob", soaList.get(1).name()),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(18, "Bob")), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(2))
    );
  }

  @Test
  public void addIndexAndGet() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(0, new Person(18, "Bob"));
    assertAll(
        () -> assertEquals(18, soaList.get(0).age()),
        () -> assertEquals("Bob", soaList.get(0).name()),
        () -> assertEquals(36, soaList.get(1).age()),
        () -> assertEquals("Ana", soaList.get(1).name()),
        () -> assertEquals(List.of(
            new Person(18, "Bob"),
            new Person(36, "Ana")
            ), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(2)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.add(-1, new Person(77, "Zip"))),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.add(3, new Person(77, "Zip"))),
        () -> assertThrows(NullPointerException.class, () -> soaList.add(0, null))
    );
  }

  @Test
  public void addIndexLast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(1, new Person(18, "Bob"));
    assertAll(
        () -> assertEquals(36, soaList.get(0).age()),
        () -> assertEquals("Ana", soaList.get(0).name()),
        () -> assertEquals(18, soaList.get(1).age()),
        () -> assertEquals("Bob", soaList.get(1).name()),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(18, "Bob")), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(2))
    );
  }

  @Test
  public void addAndSet() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(18, "Bob"));
    soaList.add(new Person(22, "Bob"));
    soaList.set(1, new Person(24, "Elo"));
    assertAll(
        () -> assertEquals(36, soaList.get(0).age()),
        () -> assertEquals("Ana", soaList.get(0).name()),
        () -> assertEquals(24, soaList.get(1).age()),
        () -> assertEquals("Elo", soaList.get(1).name()),
        () -> assertEquals(22, soaList.get(2).age()),
        () -> assertEquals("Bob", soaList.get(2).name()),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(24, "Elo"),
            new Person(22, "Bob")), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.set(-1, new Person(24, "Elo"))),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.set(3, new Person(24, "Elo"))),
        () -> assertThrows(NullPointerException.class, () -> soaList.set(1, null))
    );
  }

  @Test
  public void addResizeAndLoop() {
    var soaList = StructOfArrayList.of(Person.class);
    IntStream.range(0, 1_000_000)
        .forEach(i ->soaList.add(new Person(i, "" + i)));
    var index = 0;
    for(var person: soaList) {
      assertEquals(index, person.age());
      assertEquals("" + index, person.name());
      index++;
    }
  }

  @Test
  public void addNoResizeAndLoop() {
    var soaList = StructOfArrayList.of(Person.class, 1_000_000);
    IntStream.range(0, 1_000_000)
        .forEach(i ->soaList.add(new Person(i, "" + i)));
    var index = 0;
    for(var person: soaList) {
      assertEquals(index, person.age());
      assertEquals("" + index, person.name());
      index++;
    }
  }

  @Test
  public void addIndexResizeAndLoop() {
    var soaList = StructOfArrayList.of(Person.class);
    IntStream.range(0, 1_000)
        .forEach(i ->soaList.add(0, new Person(i, "" + i)));
    var index = 999;
    for(var person: soaList) {
      assertEquals(index, person.age());
      assertEquals("" + index, person.name());
      index--;
    }
  }

  @Test
  public void addNull() {
    var soaList = StructOfArrayList.of(Person.class);
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> soaList.add(null)),
        () -> assertThrows(NullPointerException.class, () -> soaList.add(0, null))
    );
  }

  @Test
  public void addAndRemoveIndex() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(18, "Bob"));
    var removed = soaList.remove(0);
    assertAll(
        () -> assertEquals(36, removed.age()),
        () -> assertEquals("Ana", removed.name()),
        () -> assertEquals(18, soaList.get(0).age()),
        () -> assertEquals("Bob", soaList.get(0).name()),
        () -> assertEquals(List.of(new Person(18, "Bob")), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(1))
    );
  }

  @Test
  public void removeIndexOutOfBounds() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    assertAll(
        () -> assertEquals(List.of(new Person(36, "Ana")), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.remove(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.remove(1))
    );
  }

  @Test
  public void addAndRemoveObject() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Elo"));
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(18, "Bob"));
    var result = soaList.remove(new Person(36, "Ana"));
    assertAll(
        () -> assertTrue(result),
        () -> assertEquals(36, soaList.get(0).age()),
        () -> assertEquals("Elo", soaList.get(0).name()),
        () -> assertEquals(18, soaList.get(1).age()),
        () -> assertEquals("Bob", soaList.get(1).name()),
        () -> assertEquals(List.of(
            new Person(36, "Elo"),
            new Person(18, "Bob")
            ), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(2)),
        () -> assertThrows(NullPointerException.class, () -> soaList.remove(null))
    );
  }

  @Test
  public void addAndRemoveObjectNotFound() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(77, "Elo"));
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(18, "Bob"));
    var result = soaList.remove(new Person(77, "Ana"));
    assertAll(
        () -> assertFalse(result),
        () -> assertEquals(77, soaList.get(0).age()),
        () -> assertEquals("Elo", soaList.get(0).name()),
        () -> assertEquals(36, soaList.get(1).age()),
        () -> assertEquals("Ana", soaList.get(1).name()),
        () -> assertEquals(18, soaList.get(2).age()),
        () -> assertEquals("Bob", soaList.get(2).name()),
        () -> assertEquals(List.of(
            new Person(77, "Elo"),
            new Person(36, "Ana"),
            new Person(18, "Bob")
        ), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(3))
    );
  }

  @Test
  public void addAndRemoveObjectInvalidType() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Elo"));
    soaList.add(new Person(18, "Bob"));
    var result = soaList.remove("hello");
    assertAll(
        () -> assertFalse(result),
        () -> assertEquals(36, soaList.get(0).age()),
        () -> assertEquals("Elo", soaList.get(0).name()),
        () -> assertEquals(18, soaList.get(1).age()),
        () -> assertEquals("Bob", soaList.get(1).name()),
        () -> assertEquals(List.of(
            new Person(36, "Elo"),
            new Person(18, "Bob")
        ), soaList)
    );
  }

  @Test
  public void removeBySwappingLast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(77, "Elo"));
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(18, "Bob"));
    var removed = soaList.removeBySwappingLast(0);
    assertAll(
        () -> assertEquals(2, soaList.size()),
        () -> assertEquals(77, removed.age()),
        () -> assertEquals("Elo", removed.name()),
        () -> assertEquals(18, soaList.get(0).age()),
        () -> assertEquals("Bob", soaList.get(0).name()),
        () -> assertEquals(36, soaList.get(1).age()),
        () -> assertEquals("Ana", soaList.get(1).name()),
        () -> assertEquals(List.of(
            new Person(18, "Bob"),
            new Person(36, "Ana")
            ), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(2))
    );
  }

  @Test
  public void removeBySwappingLastOnlyOneElement() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(77, "Elo"));
    var removed = soaList.removeBySwappingLast(0);
    assertAll(
        () -> assertEquals(0, soaList.size()),
        () -> assertEquals(77, removed.age()),
        () -> assertEquals("Elo", removed.name()),
        () -> assertEquals(List.of(), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(0))
    );
  }

  @Test
  public void contains() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    soaList.add(new Person(18, "Bob"));
    soaList.add(new Person(77, "Elo"));
    var result = soaList.contains(new Person(77, "Elo"));
    assertAll(
        () -> assertTrue(result),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(77, "Elo"),
            new Person(18, "Bob"),
            new Person(77, "Elo")
        ), soaList),
        () -> assertThrows(NullPointerException.class, () -> soaList.contains(null))
    );
  }

  @Test
  public void containsInvalidType() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    var result = soaList.contains("foo");
    assertAll(
        () -> assertFalse(result),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(77, "Elo")
        ), soaList)
    );
  }

  @Test
  public void containsNotFound() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    soaList.add(new Person(18, "Bob"));
    soaList.add(new Person(77, "Elo"));
    var result = soaList.contains(new Person(18, "Elo"));
    assertAll(
        () -> assertFalse(result),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(77, "Elo"),
            new Person(18, "Bob"),
            new Person(77, "Elo")), soaList)
    );
  }

  @Test
  public void indexOf() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    soaList.add(new Person(18, "Bob"));
    soaList.add(new Person(77, "Elo"));
    var index = soaList.indexOf(new Person(77, "Elo"));
    assertAll(
        () -> assertEquals(1, index),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(77, "Elo"),
            new Person(18, "Bob"),
            new Person(77, "Elo")
        ), soaList),
        () -> assertThrows(NullPointerException.class, () -> soaList.indexOf(null))
    );
  }

  @Test
  public void indexOfInvalidType() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    var index = soaList.indexOf("foo");
    assertAll(
        () -> assertEquals(-1, index),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(77, "Elo")
        ), soaList)
    );
  }

  @Test
  public void indexOfNotFound() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    soaList.add(new Person(18, "Bob"));
    soaList.add(new Person(77, "Elo"));
    var index = soaList.indexOf(new Person(18, "Elo"));
    assertAll(
        () -> assertEquals(-1, index),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(77, "Elo"),
            new Person(18, "Bob"),
            new Person(77, "Elo")), soaList)
    );
  }

  @Test
  public void lastIndexOf() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    soaList.add(new Person(18, "Bob"));
    soaList.add(new Person(77, "Elo"));
    var index = soaList.lastIndexOf(new Person(77, "Elo"));
    assertAll(
        () -> assertEquals(3, index),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(77, "Elo"),
            new Person(18, "Bob"),
            new Person(77, "Elo")), soaList),
        () -> assertThrows(NullPointerException.class, () -> soaList.lastIndexOf(null))
    );
  }

  @Test
  public void lastIndexOfInvalidType() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    var index = soaList.lastIndexOf("foo");
    assertAll(
        () -> assertEquals(-1, index),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(77, "Elo")
        ), soaList)
    );
  }

  @Test
  public void lastIndexOfNotFound() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    soaList.add(new Person(18, "Bob"));
    soaList.add(new Person(77, "Elo"));
    var index = soaList.lastIndexOf(new Person(18, "Elo"));
    assertAll(
        () -> assertEquals(-1, index),
        () -> assertEquals(List.of(
            new Person(36, "Ana"),
            new Person(77, "Elo"),
            new Person(18, "Bob"),
            new Person(77, "Elo")), soaList)
    );
  }

  @Test
  public void clear() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    soaList.clear();
    assertAll(
        () -> assertTrue(soaList.isEmpty()),
        () -> assertEquals(0, soaList.size()),
        () -> assertEquals(List.of(), soaList),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.get(0))
    );
  }

  @Test
  public void iterator() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    var iterator = soaList.iterator();
    assertEquals(new Person(36, "Ana"), iterator.next());
    assertAll(
        () -> assertTrue(iterator.hasNext()),
        () -> assertThrows(UnsupportedOperationException.class, iterator::remove)
    );
    assertEquals(new Person(77, "Elo"), iterator.next());
    assertAll(
        () -> assertFalse(iterator.hasNext()),
        () -> assertThrows(NoSuchElementException.class, iterator::next),
        () -> assertThrows(UnsupportedOperationException.class, iterator::remove)
    );
  }

  @Test
  public void iteratorAddFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.iterator();
    soaList.add(new Person(77, "Elo"));
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void iteratorAddIndexFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.iterator();
    soaList.add(0, new Person(77, "Elo"));
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void iteratorRemoveFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.iterator();
    soaList.remove(new Person(36, "Ana"));
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void iteratorRemoveIndexFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.iterator();
    soaList.remove(0);
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void listIterator() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    assertAll(
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.listIterator(-1)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> soaList.listIterator(3))
    );
    var iterator = soaList.listIterator();
    assertEquals(new Person(36, "Ana"), iterator.next());
    assertAll(
        () -> assertEquals(1, iterator.nextIndex()),
        () -> assertTrue(iterator.hasNext()),
        () -> assertThrows(UnsupportedOperationException.class, iterator::remove),
        () -> assertThrows(UnsupportedOperationException.class, () -> iterator.add(new Person(2, "Zip")))
    );
    assertEquals(new Person(77, "Elo"), iterator.next());
    assertAll(
        () -> assertFalse(iterator.hasNext()),
        () -> assertThrows(NoSuchElementException.class, iterator::next),
        () -> assertThrows(UnsupportedOperationException.class, iterator::remove),
        () -> assertThrows(UnsupportedOperationException.class, () -> iterator.add(new Person(2, "Zip")))
    );
  }

  @Test
  public void listIteratorAddFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.listIterator();
    soaList.add(new Person(77, "Elo"));
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void listIteratorAddIndexFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.listIterator();
    soaList.add(0, new Person(77, "Elo"));
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void listIteratorRemoveFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.listIterator();
    soaList.remove(new Person(36, "Ana"));
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void listIteratorRemoveIndexFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.listIterator();
    soaList.remove(0);
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void listIteratorPreviousAddFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.listIterator(1);
    soaList.add(new Person(77, "Elo"));
    assertThrows(ConcurrentModificationException.class, iterator::previous);
  }

  @Test
  public void listIteratorPreviousAddIndexFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.listIterator(1);
    soaList.add(0, new Person(77, "Elo"));
    assertThrows(ConcurrentModificationException.class, iterator::previous);
  }

  @Test
  public void listIteratorPreviousRemoveFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.listIterator(1);
    soaList.remove(new Person(36, "Ana"));
    assertThrows(ConcurrentModificationException.class, iterator::previous);
  }

  @Test
  public void listIteratorPreviousRemoveIndexFailFast() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.listIterator(1);
    soaList.remove(0);
    assertThrows(ConcurrentModificationException.class, iterator::previous);
  }

  @Test
  public void listIteratorReverse() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    var iterator = soaList.listIterator(2);
    assertEquals(new Person(77, "Elo"), iterator.previous());
    assertAll(
        () -> assertEquals(0, iterator.previousIndex()),
        () -> assertTrue(iterator.hasPrevious()),
        () -> assertThrows(UnsupportedOperationException.class, iterator::remove)
    );
    assertEquals(new Person(36, "Ana"), iterator.previous());
    assertAll(
        () -> assertFalse(iterator.hasPrevious()),
        () -> assertThrows(NoSuchElementException.class, iterator::previous),
        () -> assertThrows(UnsupportedOperationException.class, iterator::remove)
    );
  }

  @Test
  public void listIteratorSet() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    var iterator = soaList.listIterator();
    while(iterator.hasNext()) {
      var person = iterator.next();
      iterator.set(new Person(person.age() + 1, person.name()));
    }
    assertEquals(List.of(
        new Person(37, "Ana"),
        new Person(78, "Elo")
    ), soaList);
  }

  @Test
  public void listIteratorSetPreconditions() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    var iterator = soaList.listIterator();
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> iterator.set(null)),
        () -> assertThrows(IllegalStateException.class, () -> iterator.set(new Person(28, "Bob")))
    );
  }

  @Test
  public void listIteratorReverseSet() {
    var soaList = StructOfArrayList.of(Person.class);
    soaList.add(new Person(36, "Ana"));
    soaList.add(new Person(77, "Elo"));
    var iterator = soaList.listIterator(2);
    while(iterator.hasPrevious()) {
      var person = iterator.previous();
      iterator.set(new Person(person.age() + 1, person.name()));
    }
    assertEquals(List.of(
        new Person(37, "Ana"),
        new Person(78, "Elo")
    ), soaList);
  }
}