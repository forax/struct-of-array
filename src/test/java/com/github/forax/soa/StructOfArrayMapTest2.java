package com.github.forax.soa;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StructOfArrayMapTest2 {
  public record EmptyRecord() {}

  @Test
  public void emptyRecord() {
    var soaMap = StructOfArrayMap.of(EmptyRecord.class);
    soaMap.put(7, new EmptyRecord());
    soaMap.put(42, new EmptyRecord());
    assertAll(
        () -> assertEquals(2, soaMap.size()),
        () -> assertEquals(new EmptyRecord(), soaMap.get(7)),
        () -> assertEquals(new EmptyRecord(), soaMap.get(42)),
        () -> assertTrue(soaMap.containsKey(7)),
        () -> assertTrue(soaMap.containsKey(42)),
        () -> assertEquals(List.of(new EmptyRecord(), new EmptyRecord()), soaMap.values()),
        () -> assertTrue(soaMap.containsValue(new EmptyRecord()))
    );
  }

  public record Point(int x, int y) {}

  @Test
  public void pointPut() {
    var soaMap = StructOfArrayMap.of(Point.class);
    soaMap.put(1, new Point(1, 3));
    soaMap.put(14, new Point(14, 51));
    assertAll(
        () -> assertEquals(2, soaMap.size()),
        () -> assertEquals(new Point(1, 3), soaMap.get(1)),
        () -> assertEquals(new Point(14, 51), soaMap.get(14)),
        () -> assertTrue(soaMap.containsKey(1)),
        () -> assertTrue(soaMap.containsKey(14)),
        () -> assertEquals(List.of(new Point(1, 3), new Point(14, 51)), soaMap.values()),
        () -> assertTrue(soaMap.containsValue(new Point(1, 3))),
        () -> assertTrue(soaMap.containsValue(new Point(14, 51)))
    );
  }

  public record RecordWithDoubleAndFloat(double d, float f) { }

  //@Test  // FIXME
  public void withDoubleAndFloat() {
    var soaMap = StructOfArrayMap.of(RecordWithDoubleAndFloat.class);
    soaMap.put(19, new RecordWithDoubleAndFloat(2.0, 1f));
    soaMap.put(42, new RecordWithDoubleAndFloat(4.0, 2f));
    assertAll(
        () -> assertEquals(2, soaMap.size()),
        () -> assertEquals(new RecordWithDoubleAndFloat(2.0, 1f), soaMap.get(19)),
        () -> assertEquals(new RecordWithDoubleAndFloat(4.0, 2f), soaMap.get(42)),
        () -> assertTrue(soaMap.containsKey(10)),
        () -> assertTrue(soaMap.containsKey(42)),
        () -> assertEquals(List.of(new RecordWithDoubleAndFloat(2.0, 1f), new RecordWithDoubleAndFloat(4.0, 2f)), soaMap.values()),
        () -> assertTrue(soaMap.containsValue(new RecordWithDoubleAndFloat(2.0, 1f))),
        () -> assertTrue(soaMap.containsValue(new RecordWithDoubleAndFloat(4.0, 2f)))
    );
  }
}