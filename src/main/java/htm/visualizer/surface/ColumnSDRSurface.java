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

  private final static Color LIGHT_BLUE = new Color(153, 204, 255);

  protected final Region region;
  private Column currentColumn; //clicked on
  private Integer selectedColumnIndex; //selected from neighbours  table

  public ColumnSDRSurface(Region region) {
    super(region.getDimension().width, region.getDimension().height);
    this.region = region;
  }

  @Override protected void drawElement(Graphics2D g2d, int index, int x, int y, int width, int height) {
    g2d.setColor(getColumn(index).isActive() ? activeColor : this.getBackground());
    g2d.fillOval(x, y, width, height);

    //TODO REMOVE
    if (getColumn(index).isMarked()) {
      g2d.setColor(Color.RED);
      g2d.fillOval(x, y, width, height);
    }

    super.drawElement(g2d, index, x, y, width,
                      height);
  }

  public Column getColumn(int columnIndex) {
    return region.getElementByIndex(columnIndex);
  }

  public void setCurrentColumn(Column currentColumn) {
    this.currentColumn = this.currentColumn != currentColumn ? currentColumn : null;
    this.selectedColumnIndex = null;
    this.repaint();
  }

  public void setSelectedColumn(int selectedColumnIndex) {
    this.selectedColumnIndex = selectedColumnIndex;
    this.repaint();
  }

  public void drawInhibitedColumns(Column currentColumn, Graphics2D g2d) {
    java.util.List<Column> inhibitedColumn = currentColumn.getNeighbors(region.getAverageReceptiveFieldSize());
    for (Column column : inhibitedColumn) {
      Rectangle columnRec = this.getElementArea(column.getPosition());
      g2d.setColor(Color.LIGHT_GRAY);
      Composite original = g2d.getComposite();
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                  0.4f));
      g2d.fillOval(columnRec.x, columnRec.y, columnRec.width, columnRec.height);
      g2d.setComposite(original);
    }
  }

  @Override protected void doDrawing(Graphics2D g2d) {
    super.doDrawing(g2d);
    if (currentColumn != null) {
      drawInhibitedColumns(currentColumn, g2d);
    }
    if (selectedColumnIndex != null) {
      Composite original = g2d.getComposite();
      Rectangle aroundRec = getElementAreaWithScale(selectedColumnIndex, 1 / (Math.PI / 4) * 1.5);
      g2d.setColor(LIGHT_BLUE);
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                  0.5f));
      g2d.fillOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
      g2d.setComposite(original);
    }
  }
}
