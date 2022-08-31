package com.github.forax.soa.integration;

import com.github.forax.soa.StructOfArrayMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StructOfArrayMapTest3 {
  @Test
  public void pointPut() {
    record Point(int x, int y) {}

    var soaMap = StructOfArrayMap.of(lookup(), Point.class);
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
}