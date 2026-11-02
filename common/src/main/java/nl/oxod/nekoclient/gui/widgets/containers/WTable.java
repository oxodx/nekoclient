/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.widgets.containers;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import nl.oxod.nekoclient.gui.utils.Cell;
import nl.oxod.nekoclient.gui.widgets.WWidget;

import java.util.*;

public class WTable extends WContainer {
  public double horizontalSpacing = 3;
  public double verticalSpacing = 3;

  private final List<List<Cell<?>>> rows = new ArrayList<>();
  private int rowI;
  private boolean stacked;

  private final DoubleList rowHeights = new DoubleArrayList();
  private final DoubleList columnWidths = new DoubleArrayList();

  private final DoubleList rowWidths = new DoubleArrayList();
  private final IntList rowExpandCellXCounts = new IntArrayList();

  @Override
  public <T extends WWidget> Cell<T> add(T widget) {
    Cell<T> cell = super.add(widget);

    if (rows.size() <= rowI) {
      List<Cell<?>> row = new ArrayList<>();
      row.add(cell);
      rows.add(row);
    } else rows.get(rowI).add(cell);

    return cell;
  }

  public void row() {
    rowI++;
  }

  public int rowI() {
    return rowI;
  }

  public void removeRow(int i) {
    for (Cell<?> cell : rows.remove(i)) {
      for (Iterator<Cell<?>> it = cells.iterator(); it.hasNext(); ) {
        if (it.next() == cell) {
          it.remove();
          break;
        }
      }
    }

    rowI--;
  }

  public List<Cell<?>> getRow(int i) {
    if (i < 0 || i >= rows.size()) return null;
    return rows.get(i);
  }

  @Override
  public void clear() {
    super.clear();
    rows.clear();
    rowI = 0;
  }

  protected double horizontalSpacing() {
    return theme.scale(horizontalSpacing);
  }

  protected double verticalSpacing() {
    return theme.scale(verticalSpacing);
  }

  @Override
  protected void onCalculateSize() {
    calculateInfo();

    // Reset
    rowWidths.clear();

    width = 0;
    height = 0;

    // Loop over rows
    for (int rowI = 0; rowI < rows.size(); rowI++) {
      List<Cell<?>> row = rows.get(rowI);

      double rowWidth = 0;

      // Loop over cells in the row
      for (int cellI = 0; cellI < row.size(); cellI++) {
        // Calculate row width
        if (cellI > 0) rowWidth += horizontalSpacing();
        rowWidth += columnWidths.getDouble(cellI);
      }

      // Store row width
      rowWidths.add(rowWidth);
      width = Math.max(width, rowWidth);

      // Calculate height
      if (rowI > 0) height += verticalSpacing();
      height += rowHeights.getDouble(rowI);
    }

    stacked = width > availableWidth;
    if (stacked) {
      width = availableWidth;
      height = 0;
      for (List<Cell<?>> row : rows) {
        if (row.isEmpty()) continue;
        if (height > 0) height += verticalSpacing() * 2;
        Cell<?> label = row.getFirst();
        height += label.padTop() + label.widget().height + label.padBottom();
        double controlsHeight = 0;
        for (int i = 1; i < row.size(); i++) {
          Cell<?> cell = row.get(i);
          controlsHeight = Math.max(controlsHeight, cell.padTop() + cell.widget().height + cell.padBottom());
        }
        if (row.size() > 1) height += verticalSpacing() + controlsHeight;
      }
    }
  }

  @Override
  protected void onCalculateWidgetPositions() {
    if (stacked) {
      double nextY = y;
      for (List<Cell<?>> row : rows) {
        if (row.isEmpty()) continue;
        if (nextY > y) nextY += verticalSpacing() * 2;
        Cell<?> label = row.getFirst();
        label.x = x + label.padLeft();
        label.y = nextY + label.padTop();
        label.width = Math.max(0, width - label.padLeft() - label.padRight());
        label.height = label.widget().height;
        label.alignWidget();
        nextY += label.padTop() + label.height + label.padBottom();
        if (row.size() <= 1) continue;
        nextY += verticalSpacing();

        double fixed = horizontalSpacing() * Math.max(0, row.size() - 2);
        int flexible = 0;
        for (int i = 1; i < row.size(); i++) {
          Cell<?> cell = row.get(i);
          fixed += cell.padLeft() + cell.padRight();
          if (cell.expandCellX) flexible++;
          else fixed += cell.widget().width;
        }
        double flexWidth = flexible > 0 ? Math.max(0, width - fixed) / flexible : 0;
        double nextX = x;
        double controlsHeight = 0;
        for (int i = 1; i < row.size(); i++) {
          Cell<?> cell = row.get(i);
          cell.x = nextX + cell.padLeft();
          cell.y = nextY + cell.padTop();
          cell.width = Math.max(0, Math.min(cell.expandCellX ? flexWidth : cell.widget().width, x + width - cell.x));
          cell.height = cell.widget().height;
          cell.alignWidget();
          nextX += cell.padLeft() + cell.width + cell.padRight() + horizontalSpacing();
          controlsHeight = Math.max(controlsHeight, cell.padTop() + cell.height + cell.padBottom());
        }
        nextY += controlsHeight;
      }
      return;
    }
    double y = this.y;

    // Loop over rows
    for (int rowI = 0; rowI < rows.size(); rowI++) {
      List<Cell<?>> row = rows.get(rowI);

      if (rowI > 0) y += verticalSpacing();

      double x = this.x;
      double rowHeight = rowHeights.getDouble(rowI);

      double expandXAdd = rowExpandCellXCounts.getInt(rowI) > 0 ? (width - rowWidths.getDouble(rowI)) / rowExpandCellXCounts.getInt(rowI) : 0;

      // Loop over cells in the row
      for (int cellI = 0; cellI < row.size(); cellI++) {
        Cell<?> cell = row.get(cellI);

        if (cellI > 0) x += horizontalSpacing();
        double columnWidth = columnWidths.getDouble(cellI);

        cell.x = x;
        cell.y = y;

        cell.width = columnWidth + (cell.expandCellX ? expandXAdd : 0);
        cell.height = rowHeight;

        cell.alignWidget();

        x += columnWidth + (cell.expandCellX ? expandXAdd : 0);
      }

      y += rowHeight;
    }
  }

  private void calculateInfo() {
    // Reset
    rowHeights.clear();
    columnWidths.clear();
    rowExpandCellXCounts.clear();

    Map<String, IntList> columnGroups = new HashMap<>();

    // Loop over rows
    for (List<Cell<?>> row : rows) {
      double rowHeight = 0;
      int rowExpandXCount = 0;

      // Loop over cells in the row
      for (int i = 0; i < row.size(); i++) {
        Cell<?> cell = row.get(i);

        // Calculate row height
        rowHeight = Math.max(rowHeight, cell.padTop() + cell.widget().height + cell.padBottom());

        // Calculate column width
        double cellWidth = cell.padLeft() + cell.widget().width + cell.padRight();
        if (columnWidths.size() <= i) columnWidths.add(cellWidth);
        else columnWidths.set(i, Math.max(columnWidths.getDouble(i), cellWidth));

        if (cell.group != null) columnGroups.computeIfAbsent(cell.group, _ -> new IntArrayList()).add(i);

        // Calculate row expandX count
        if (cell.expandCellX) rowExpandXCount++;
      }

      // Store calculated info
      rowHeights.add(rowHeight);
      rowExpandCellXCounts.add(rowExpandXCount);
    }

    // Normalize group widths
    columnGroups.values().forEach(columns -> {
      double maxWidth = Integer.MIN_VALUE;
      for (int i : columns) {
        maxWidth = Math.max(maxWidth, columnWidths.getDouble(i));
      }

      for (int i : columns) {
        columnWidths.set(i, maxWidth);
      }
    });
  }
}
