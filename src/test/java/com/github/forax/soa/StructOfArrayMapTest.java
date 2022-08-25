package com.github.forax.soa;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class StructOfArrayMapTest {
  @Test
  public void of() {
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> StructOfArrayMap.of(null)),
        () -> assertThrows(NullPointerException.class, () -> StructOfArrayMap.of(Person.class, null)),
        () -> assertThrows(NullPointerException.class, () -> StructOfArrayMap.of(null, Map.of())),
        () -> assertThrows(NullPointerException.class, () -> StructOfArrayMap.of(null, 8)),
        () -> assertThrows(IllegalArgumentException.class, () -> StructOfArrayMap.of(Person.class, -1))
    );
  }

  @Test
  public void ofMap() {
    var soaMap = StructOfArrayMap.of(Person.class,
        Map.of(
            1, new Person(1, "A"),
            17, new Person(2, "B"),
            33, new Person(3, "C")
        ));
    assertAll(
        () -> assertEquals(new Person(1, "A"), soaMap.get(1)),
        () -> assertEquals(new Person(2, "B"), soaMap.get(17)),
        () -> assertEquals(new Person(3, "C"), soaMap.get(33))
    );
  }

  @Test
  public void empty() {
    var soaMap = StructOfArrayMap.of(Person.class);
    assertAll(
        () -> assertEquals(0, soaMap.size()),
        () -> assertTrue(soaMap.isEmpty()),
        () -> assertEquals(Map.of(), soaMap),
        () -> assertNull(soaMap.get("foo")),
        () -> assertNull(soaMap.get(3)),
        () -> assertNull(soaMap.getOrDefault("bar", null)),
        () -> assertNull(soaMap.getOrDefault(12, null))
    );
  }

  @Test
  public void putAndGet() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(21, new Person(21, "Ana"));
    soaMap.put(20, new Person(20, "Bob"));
    assertAll(
        () -> assertEquals(2, soaMap.size()),
        () -> assertFalse(soaMap.isEmpty()),
        () -> assertEquals(
            Map.of(21, new Person(21, "Ana"), 20, new Person(20, "Bob")),
            soaMap),
        () -> assertEquals(new Person(21, "Ana"), soaMap.get(21)),
        () -> assertEquals(new Person(20, "Bob"), soaMap.get(20)),
        () -> assertEquals(new Person(21, "Ana"), soaMap.getOrDefault(21, null)),
        () -> assertEquals(new Person(20, "Bob"), soaMap.getOrDefault(20, null))
    );
  }

  @Test
  public void putSameKey() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(32, new Person(21, "Ana"));
    soaMap.put(32, new Person(20, "Bob"));
    assertAll(
        () -> assertEquals(1, soaMap.size()),
        () -> assertFalse(soaMap.isEmpty()),
        () -> assertEquals(
            Map.of(32, new Person(20, "Bob")),
            soaMap),
        () -> assertEquals(new Person(20, "Bob"), soaMap.get(32)),
        () -> assertEquals(new Person(20, "Bob"), soaMap.getOrDefault(32, null))
    );
  }

  @Test
  public void putPreconditions() {
    var soaMap = StructOfArrayMap.of(Person.class);
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> soaMap.put(null, new Person(34, "Ana"))),
        () -> assertThrows(NullPointerException.class, () -> soaMap.put(3, null))
    );
  }

  @Test
  public void putCollisions() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(1, new Person(1, "A"));
    soaMap.put(17, new Person(2, "B"));
    soaMap.put(33, new Person(3, "C"));
    assertAll(
        () -> assertEquals(new Person(1, "A"), soaMap.get(1)),
        () -> assertEquals(new Person(2, "B"), soaMap.get(17)),
        () -> assertEquals(new Person(3, "C"), soaMap.get(33)),
        () -> assertTrue(soaMap.containsKey(1)),
        () -> assertTrue(soaMap.containsKey(17)),
        () -> assertTrue(soaMap.containsKey(33))
    );
  }

  @Test
  public void putResizeAndGet() {
    var soaMap = StructOfArrayMap.of(Person.class);
    IntStream.range(0, 10_000_000)
            .forEach(i -> soaMap.put(i, new Person(i, "" + i)));
    for(var i = 0; i < soaMap.size(); i++) {
      assertEquals(new Person(i, "" + i), soaMap.get(i));
    }
  }

  @Test
  public void putResizeCollisions() {
    var soaMap = StructOfArrayMap.of(Person.class);
    IntStream.iterate(1, x -> x + 32)
            .limit(40)
            .forEach(i -> soaMap.put(i, new Person(i, "" + i)));
    IntStream.iterate(1, x -> x + 32)
        .limit(40)
        .forEach(i -> {
          assertEquals(new Person(i, "" + i), soaMap.get(i));
          assertTrue(soaMap.containsKey(i));
        });
  }

  @Test
  public void putNoResize() {
    var soaMap = StructOfArrayMap.of(Person.class, 10_000_000);
    IntStream.range(0, 10_000_000)
        .forEach(i -> soaMap.put(i, new Person(i, "" + i)));
  }

  @Test
  public void getPreconditions() {
    var soaMap = StructOfArrayMap.of(Person.class);
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> soaMap.get(null)),
        () -> assertThrows(NullPointerException.class, () -> soaMap.getOrDefault(null, new Person(16, "Ana")))
    );
  }

  @Test
  public void containsValue() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(10, new Person(10, "Ana"));
    soaMap.put(6, new Person(12, "Bob"));
    assertAll(
        () -> assertTrue(soaMap.containsValue(new Person(10, "Ana"))),
        () -> assertTrue(soaMap.containsValue(new Person(12, "Bob"))),
        () -> assertFalse(soaMap.containsValue(new Person(10, "Elo"))),
        () -> assertFalse(soaMap.containsValue(new Person(12, "Ana"))),
        () -> assertFalse(soaMap.containsValue("foo")),
        () -> assertThrows(NullPointerException.class, () -> soaMap.containsValue(null))
    );
  }

  @Test
  public void containsKey() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(10, new Person(10, "Ana"));
    soaMap.put(6, new Person(12, "Bob"));
    assertAll(
        () -> assertTrue(soaMap.containsKey(10)),
        () -> assertTrue(soaMap.containsKey(6)),
        () -> assertFalse(soaMap.containsKey(12)),
        () -> assertFalse(soaMap.containsKey(-1)),
        () -> assertFalse(soaMap.containsKey("foo")),
        () -> assertThrows(NullPointerException.class, () -> soaMap.containsKey(null))
    );
  }

  @Test
  public void putResizeAndContainsKey() {
    var soaMap = StructOfArrayMap.of(Person.class);
    IntStream.range(0, 10_000_000)
        .forEach(i -> soaMap.put(i, new Person(i, "" + i)));
    for(var i = 0; i < soaMap.size(); i++) {
      assertTrue(soaMap.containsKey(i));
    }
  }

  @Test
  public void forEach() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(10, new Person(10, "Ana"));
    soaMap.put(6, new Person(12, "Bob"));
    soaMap.forEach((integer, person) -> {
      switch (integer) {
        case 10 -> assertEquals(new Person(10, "Ana"), person);
        case 6 -> assertEquals(new Person(12, "Bob"), person);
        default -> throw new AssertionError();
      }
    });
  }

  @Test
  public void forEachEmpty() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.forEach((integer, person) -> fail());
    assertThrows(NullPointerException.class, () -> soaMap.forEach(null));
  }

  @Test
  public void putResizeAndForEach() {
    var soaMap = StructOfArrayMap.of(Person.class);
    IntStream.range(0, 10_000_000)
        .forEach(i -> soaMap.put(i, new Person(i, "" + i)));
    var box = new Object() { int index; };
    soaMap.forEach((integer, person) -> {
      assertAll(
          () -> assertEquals(box.index, integer),
          () -> assertEquals("" + box.index, person.name()),
          () -> assertEquals(box.index, person.age())
      );
      box.index++;
    });
  }

  @Test
  public void removeNoEntry() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(48, new Person(54, "Ana"));
    assertAll(
        () -> assertNull(soaMap.remove(42)),
        () -> assertNull(soaMap.remove("foo")),
        () -> assertNull(soaMap.remove(0)),
        () -> assertNull(soaMap.remove(16)),
        () -> assertNull(soaMap.remove(32)),
        () -> assertThrows(NullPointerException.class, () -> soaMap.remove(null))
    );
  }

  @Test
  public void removeNoEntryAnymore() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(48, new Person(54, "Ana"));
    var result = soaMap.remove(48);
    assertAll(
        () -> assertEquals(0, soaMap.size()),
        () -> assertEquals(new Person(54, "Ana"), result),
        () -> assertNull(soaMap.get(48)),
        () -> assertNull(soaMap.remove(48))
    );
  }

  @Test
  public void removeTombstoneWorks() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.put(16, new Person(2, "B"));
    soaMap.put(32, new Person(3, "C"));
    var result = soaMap.remove(16);
    assertAll(
        () -> assertEquals(2, soaMap.size()),
        () -> assertEquals(new Person(2, "B"), result),
        () -> assertEquals(List.of(
            new Person(1, "A"),
            new Person(3, "C")
        ), soaMap.values()),
        () -> assertEquals(new Person(1, "A"), soaMap.get(0)),
        () -> assertEquals(new Person(3, "C"), soaMap.get(32))
    );
  }

  @Test
  public void removeTombstoneWorks2() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.put(16, new Person(2, "B"));
    soaMap.put(32, new Person(3, "C"));
    var result = soaMap.remove(0);
    assertAll(
        () -> assertEquals(2, soaMap.size()),
        () -> assertEquals(List.of(
                new Person(3, "C"),
                new Person(2, "B")
            ),
            soaMap.values()),
        () -> assertEquals(new Person(3, "C"), soaMap.get(32)),
        () -> assertEquals(new Person(2, "B"), soaMap.get(16))
    );
  }

  @Test
  public void clear() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.clear();
    assertAll(
        () -> assertTrue(soaMap.isEmpty()),
        () -> assertEquals(0, soaMap.size()),
        () -> assertEquals(Map.of(), soaMap)
    );
  }

  @Test
  public void replaceTombstoneWorks() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.put(16, new Person(2, "B"));
    soaMap.put(7, new Person(3, "C"));
    soaMap.remove(0);
    var result = soaMap.replace(16, new Person(99, "Z"));
    assertAll(
        () -> assertEquals(2, soaMap.size()),
        () -> assertEquals(new Person(2, "B"), result),
        () -> assertEquals(new Person(99, "Z"), soaMap.get(16)),
        () -> assertEquals(new Person(3, "C"), soaMap.get(7)),
        () -> assertEquals(
            List.of(
                new Person(3, "C"),
                new Person(99, "Z")),
            soaMap.values())
    );
  }

  @Test
  public void replaceNoEntry() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    var result = soaMap.replace(16, new Person(99, "Z"));
    assertAll(
        () -> assertEquals(1, soaMap.size()),
        () -> assertNull(result),
        () -> assertEquals(new Person(1, "A"), soaMap.get(0))
    );
  }

  @Test
  public void replaceNoEntryTombstone() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.remove(0);
    var result = soaMap.replace(16, new Person(99, "Z"));
    assertAll(
        () -> assertEquals(0, soaMap.size()),
        () -> assertNull(result)
    );
  }

  @Test
  public void replacePreconditions() {
    var soaMap = StructOfArrayMap.of(Person.class);
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> soaMap.replace(null, new Person(0, ""))),
        () -> assertThrows(NullPointerException.class, () -> soaMap.replace(42, null))
        );
  }

  @Test
  public void entrySet() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.put(16, new Person(2, "B"));
    soaMap.put(8, new Person(3, "C"));
    assertEquals(
        Set.of(
            Map.entry(0, new Person(1, "A")),
            Map.entry(16, new Person(2, "B")),
            Map.entry(8, new Person(3, "C"))
        ),
        soaMap.entrySet()
    );
  }

  @Test
  public void entrySetAsList() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.put(16, new Person(2, "B"));
    soaMap.put(8, new Person(3, "C"));
    assertEquals(
        List.of(
            Map.entry(0, new Person(1, "A")),
            Map.entry(16, new Person(2, "B")),
            Map.entry(8, new Person(3, "C"))
        ),
        new ArrayList<>(soaMap.entrySet())
    );
  }

  @Test
  public void entrySetEntrySetValue() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.put(16, new Person(2, "B"));
    soaMap.put(8, new Person(3, "C"));
    for (var entry : soaMap.entrySet()) {
      entry.setValue(new Person(entry.getKey(), entry.getValue().name()));
    }
    assertEquals(
        List.of(
            new Person(0, "A"),
            new Person(16, "B"),
            new Person(8, "C")
        ),
        soaMap.values()
    );
  }

  @Test
  public void entrySetEntryToString() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    var entry = soaMap.entrySet().iterator().next();
    assertEquals("0=Person[age=1, name=A]", entry.toString());
  }

  @Test
  public void entrySetIterator() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(42, new Person(1, "A"));
    var iterator = soaMap.entrySet().iterator();
    assertTrue(iterator.hasNext());
    assertEquals(42, iterator.next().getKey());
    assertFalse(iterator.hasNext());
    assertThrows(NoSuchElementException.class, iterator::next);
  }

  @Test
  public void entrySetIteratorFailFast() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(42, new Person(1, "A"));
    var iterator = soaMap.entrySet().iterator();
    soaMap.put(777, new Person(2, "B"));
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void keySet() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.put(16, new Person(2, "B"));
    soaMap.put(8, new Person(3, "C"));
    var keySet = soaMap.keySet();
    assertEquals(List.of(0, 16, 8), new ArrayList<>(keySet));
  }

  @Test
  public void keySetStructurallyUnmodifiable() {
    var soaMap = StructOfArrayMap.of(Person.class);
    var keySet = soaMap.keySet();
    assertAll(
        () -> assertThrows(UnsupportedOperationException.class, () -> keySet.add(3)),
        () -> assertThrows(UnsupportedOperationException.class, () -> keySet.remove(0))
    );
  }

  @Test
  public void keySetContains() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(1, new Person(1, "A"));
    soaMap.put(2, new Person(2, "B"));
    soaMap.put(17, new Person(3, "C"));
    var keySet = soaMap.keySet();
    assertAll(
        () -> assertTrue(keySet.contains(1)),
        () -> assertTrue(keySet.contains(2)),
        () -> assertTrue(keySet.contains(17)),
        () -> assertFalse(keySet.contains(32))
    );
  }

  @Test
  public void keySetIterator() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(42, new Person(1, "A"));
    var iterator = soaMap.keySet().iterator();
    assertTrue(iterator.hasNext());
    assertEquals(42, iterator.next());
    assertFalse(iterator.hasNext());
    assertThrows(NoSuchElementException.class, iterator::next);
  }

  @Test
  public void keySetIteratorFailFast() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(42, new Person(1, "A"));
    var iterator = soaMap.keySet().iterator();
    soaMap.put(777, new Person(2, "B"));
    assertThrows(ConcurrentModificationException.class, iterator::next);
  }

  @Test
  public void values() {
    var soaMap = StructOfArrayMap.of(Person.class);
    soaMap.put(0, new Person(1, "A"));
    soaMap.put(16, new Person(2, "B"));
    soaMap.put(8, new Person(3, "C"));
    assertEquals(List.of(
        new Person(1, "A"),
        new Person(2, "B"),
        new Person(3, "C")
    ), soaMap.values());
  }

  @Test
  public void valuesStructurallyUnmodifiable() {
    var soaMap = StructOfArrayMap.of(Person.class);
    var values = soaMap.values();
    assertAll(
        () -> assertThrows(UnsupportedOperationException.class, () -> values.add(new Person(31, "Ana"))),
        () -> assertThrows(UnsupportedOperationException.class, () -> values.remove(new Person(31, "Ana"))),
        () -> assertThrows(UnsupportedOperationException.class, () -> values.remove(0))
    );
  }
}