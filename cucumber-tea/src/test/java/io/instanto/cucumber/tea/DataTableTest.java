/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DataTableTest {
  @Test
  public void exposesCellsAndHeaderMaps() {
    DataTable table =
        DataTable.of(new String[][] {{"name", "drink"}, {"Ada", "tea"}, {"Grace", "coffee"}});

    assertEquals(3, table.height());
    assertEquals(2, table.width());
    assertEquals("tea", table.cell(1, 1));
    assertEquals(
        List.of(
            Map.of("name", "Ada", "drink", "tea"),
            Map.of("name", "Grace", "drink", "coffee")),
        table.asMaps());
  }

  @Test
  public void copiesInputCells() {
    String[][] cells = {{"name"}, {"Ada"}};

    DataTable table = DataTable.of(cells);
    cells[1][0] = "Grace";

    assertEquals("Ada", table.cell(1, 0));
    assertThrows(UnsupportedOperationException.class, () -> table.rows().get(0).add("drink"));
  }

  @Test
  public void rejectsRowsWithDifferentWidths() {
    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> DataTable.of(new String[][] {{"name", "drink"}, {"Ada"}}));

    assertEquals("Data table rows must have the same width", failure.getMessage());
  }

  @Test
  public void rejectsDuplicateHeadingsWhenMappingRows() {
    DataTable table = DataTable.of(new String[][] {{"name", "name"}, {"Ada", "Lovelace"}});

    IllegalStateException failure = assertThrows(IllegalStateException.class, table::asMaps);

    assertEquals("Data table headings must be unique", failure.getMessage());
  }
}
