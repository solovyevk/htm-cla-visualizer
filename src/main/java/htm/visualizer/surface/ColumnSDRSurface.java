/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.surface;


import htm.model.Column;

import java.awt.*;

public class ColumnSDRSurface extends BaseSurface.CircleElementsSurface {

  protected final Column[] columns;

  public ColumnSDRSurface(int xSize, int ySize, Column[] region) {
    super(xSize, ySize);
    this.columns = region;
  }

  @Override
   protected void drawElement(Graphics2D g2d, int index, int x, int y, int width, int height) {
     g2d.setColor(getColumn(index).isActive() ? activeColor : this.getBackground());
     g2d.fillRect(x, y, width, height);
     g2d.setColor(activeColor);
     g2d.drawRect(x, y, width, height);
   }

  public Column getColumn(int columnIndex) {
    return columns[columnIndex];
  }
}
