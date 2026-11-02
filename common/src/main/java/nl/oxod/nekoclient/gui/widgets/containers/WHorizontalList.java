/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.widgets.containers;

import nl.oxod.nekoclient.gui.utils.Cell;

public class WHorizontalList extends WContainer {
  public double spacing = 3;

  protected double calculatedWidth;
  protected int fillXCount;

  protected double spacing() {
    return theme.scale(spacing);
  }

  @Override
  protected void onCalculateSize() {
    width = 0;
    height = 0;

    fillXCount = 0;

    for (int i = 0; i < cells.size(); i++) {
      Cell<?> cell = cells.get(i);

      if (i > 0) width += spacing();

      width += cell.padLeft() + cell.widget().width + cell.padRight();
      height = Math.max(height, cell.padTop() + cell.widget().height + cell.padBottom());

      if (cell.expandCellX) fillXCount++;
    }

    calculatedWidth = width;
  }

  @Override
  protected void onCalculateWidgetPositions() {
    double x = this.x;
    double fillXWidth = fillXCount > 0 ? Math.max(0, width - calculatedWidth) / fillXCount : 0;
    double shrink = calculatedWidth > width && calculatedWidth > 0 ? width / calculatedWidth : 1;

    for (int i = 0; i < cells.size(); i++) {
      Cell<?> cell = cells.get(i);

      if (i > 0) x += spacing();
      x += cell.padLeft();

      cell.x = x;
      cell.y = y + cell.padTop();

      cell.width = cell.widget().width * shrink;
      cell.height = height - cell.padTop() - cell.padBottom();

      if (cell.expandCellX) cell.width += fillXWidth;
      cell.alignWidget();

      x += cell.width + cell.padRight();
    }
  }
}
