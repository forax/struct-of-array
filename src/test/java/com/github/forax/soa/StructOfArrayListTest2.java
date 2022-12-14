package com.github.forax.soa;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StructOfArrayListTest2 {
  public record EmptyRecord() {}

  @Test
  public void emptyRecord() {
    var soaList = StructOfArrayList.of(lookup(), EmptyRecord.class);
    soaList.add(new EmptyRecord());
    soaList.add(new EmptyRecord());
    assertAll(
        () -> assertEquals(2, soaList.size()),
        () -> assertEquals(new EmptyRecord(), soaList.get(0)),
        () -> assertEquals(new EmptyRecord(), soaList.get(1)),
        () -> assertEquals(0, soaList.indexOf(new EmptyRecord())),
        () -> assertEquals(1, soaList.lastIndexOf(new EmptyRecord())),
        () -> assertTrue(soaList.contains(new EmptyRecord()))
    );
  }

  public record Point(int x, int y) {}

  @Test
  public void pointAdd() {
    var soaList = StructOfArrayList.of(lookup(), Point.class);
    soaList.add(new Point(1, 3));
    soaList.add(new Point(14, 51));
    assertAll(
        () -> assertEquals(2, soaList.size()),
        () -> assertEquals(new Point(1, 3), soaList.get(0)),
        () -> assertEquals(new Point(14, 51), soaList.get(1)),
        () -> assertEquals(0, soaList.indexOf(new Point(1, 3))),
        () -> assertEquals(1, soaList.lastIndexOf(new Point(14, 51))),
        () -> assertTrue(soaList.contains(new Point(14, 51)))
    );
  }

  public record RecordWithDoubleAndFloat(double d, float f) { }

  @Test
  public void withDoubleAndFloat() {
    var soaList = StructOfArrayList.of(lookup(), RecordWithDoubleAndFloat.class);
    soaList.add(new RecordWithDoubleAndFloat(2.0, 1f));
    soaList.add(new RecordWithDoubleAndFloat(4.0, 2f));
    assertAll(
        () -> assertEquals(2, soaList.size()),
        () -> assertEquals(new RecordWithDoubleAndFloat(2.0, 1f), soaList.get(0)),
        () -> assertEquals(new RecordWithDoubleAndFloat(4.0, 2f), soaList.get(1)),
        () -> assertEquals(0, soaList.indexOf(new RecordWithDoubleAndFloat(2.0, 1f))),
        () -> assertEquals(1, soaList.lastIndexOf(new RecordWithDoubleAndFloat(4.0, 2f))),
        () -> assertTrue(soaList.contains(new RecordWithDoubleAndFloat(4.0, 2f)))
    );
  }

  public record LongPoint(long x, long y) {}

  @Test
  public void longPointAdd() {
    var soaList = StructOfArrayList.of(lookup(), LongPoint.class);
    soaList.add(new LongPoint(1, 3));
    soaList.add(new LongPoint(14, 51));
    assertAll(
        () -> assertEquals(2, soaList.size()),
        () -> assertEquals(new LongPoint(1, 3), soaList.get(0)),
        () -> assertEquals(new LongPoint(14, 51), soaList.get(1)),
        () -> assertEquals(0, soaList.indexOf(new LongPoint(1, 3))),
        () -> assertEquals(1, soaList.lastIndexOf(new LongPoint(14, 51))),
        () -> assertTrue(soaList.contains(new LongPoint(14, 51)))
    );
  }
}