/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.surface;


import htm.model.Column;
import htm.model.Region;

import java.awt.*;

public class ColumnSDRSurface extends BaseSurface.CircleElementsSurface {

  protected final Region region;
  private Column currentColumn;

  public ColumnSDRSurface(int xSize, int ySize, Region region) {
    super(xSize, ySize);
    this.region = region;
  }

  @Override protected void drawElement(Graphics2D g2d, int index, int x, int y, int width, int height) {
    g2d.setColor(getColumn(index).isActive() ? activeColor : this.getBackground());
    Composite original = g2d.getComposite();
    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                0.6f));
    g2d.fillOval(x, y, width, height);
    g2d.setComposite(original);
    super.drawElement(g2d, index, x, y, width,
                      height);
  }

  public Column getColumn(int columnIndex) {
    return region.getElementByIndex(columnIndex);
  }

  public void setCurrentColumn(Column currentColumn) {
    this.currentColumn = this.currentColumn != currentColumn ? currentColumn : null;
  }

  public void drawInhibitedColumns(Column currentColumn, Graphics2D g2d) {
    java.util.List<Column> inhibitedColumn = currentColumn.getNeighbors(region.getAverageReceptiveFieldSize());
    for (Column column : inhibitedColumn) {
      Rectangle columnRec = this.getElementArea(column.getPosition());
      g2d.setColor(Color.BLUE);
      Composite original = g2d.getComposite();
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                  0.1f));
      g2d.fillOval(columnRec.x, columnRec.y, columnRec.width, columnRec.height);
      g2d.setComposite(original);
    }
  }

  @Override protected void doDrawing(Graphics2D g2d) {
    super.doDrawing(g2d);
    if (currentColumn != null) {
      drawInhibitedColumns(currentColumn, g2d);
    }
  }
}
