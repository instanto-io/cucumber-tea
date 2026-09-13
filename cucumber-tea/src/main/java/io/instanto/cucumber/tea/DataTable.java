/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.cucumber.tea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A portable, immutable table supplied to a step from a Gherkin data table. */
public final class DataTable {
  private final List<List<String>> rows;

  private DataTable(String[][] cells) {
    List<List<String>> copiedRows = new ArrayList<>();
    int width = cells.length == 0 ? 0 : row(cells, 0).length;
    for (int rowIndex = 0; rowIndex < cells.length; rowIndex++) {
      String[] sourceRow = row(cells, rowIndex);
      if (sourceRow.length != width) {
        throw new IllegalArgumentException("Data table rows must have the same width");
      }
      List<String> row = new ArrayList<>();
      for (String cell : sourceRow) {
        if (cell == null) {
          throw new IllegalArgumentException("Data table cells must not be null");
        }
        row.add(cell);
      }
      copiedRows.add(Collections.unmodifiableList(row));
    }
    rows = Collections.unmodifiableList(copiedRows);
  }

  /** Copies a rectangular array into an immutable table. */
  public static DataTable of(String[][] cells) {
    if (cells == null) {
      throw new IllegalArgumentException("cells must not be null");
    }
    return new DataTable(cells);
  }

  /** Returns the number of rows. */
  public int height() {
    return rows.size();
  }

  /** Returns the number of columns, or zero for an empty table. */
  public int width() {
    return rows.isEmpty() ? 0 : rows.get(0).size();
  }

  /** Returns the cell at the zero-based row and column. */
  public String cell(int row, int column) {
    return rows.get(row).get(column);
  }

  /** Returns immutable rows and cells. */
  public List<List<String>> rows() {
    return rows;
  }

  /** Treats the first row as unique column headings and maps each remaining row. */
  public List<Map<String, String>> asMaps() {
    if (rows.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> headings = rows.get(0);
    Set<String> uniqueHeadings = new HashSet<>(headings);
    if (uniqueHeadings.size() != headings.size()) {
      throw new IllegalStateException("Data table headings must be unique");
    }
    List<Map<String, String>> result = new ArrayList<>();
    for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
      List<String> values = rows.get(rowIndex);
      if (values.size() != headings.size()) {
        throw new IllegalStateException("Data table rows have different widths");
      }
      Map<String, String> row = new LinkedHashMap<>();
      for (int column = 0; column < headings.size(); column++) {
        row.put(headings.get(column), values.get(column));
      }
      result.add(Collections.unmodifiableMap(row));
    }
    return Collections.unmodifiableList(result);
  }

  private static String[] row(String[][] cells, int index) {
    if (cells[index] == null) {
      throw new IllegalArgumentException("Data table rows must not be null");
    }
    return cells[index];
  }
}
