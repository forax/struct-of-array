package com.github.forax.soa.integration;

import com.github.forax.soa.StructOfArrayList;
import org.junit.jupiter.api.Test;

import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StructOfArrayListTest3 {

  @Test
  public void pointAdd() {
    record Point(int x, int y) {}

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
}