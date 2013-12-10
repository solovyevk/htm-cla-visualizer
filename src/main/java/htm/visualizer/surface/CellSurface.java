/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.surface;

import htm.model.Cell;
import htm.model.Region;

import java.awt.*;

public abstract class CellSurface extends BaseSurface.CircleElementsSurface {
  public static final Color PREDICTED_COLOR = Color.BLUE;
  public static final Color LEARNING_COLOR = Color.RED;
  private int time = Cell.NOW;
  protected Region region;

  public CellSurface(int xSize, int ySize, Region region) {
    super(xSize, ySize);
    this.region = region;
  }

  protected Region getRegion() {
    return region;
  }

  @Override
  protected void drawElement(Graphics2D g2d, int columnIndex, int x, int y, int width, int height) {
    Cell currentCell = getCell(columnIndex);
    drawCell(g2d, x, y, width, height, currentCell, Cell.NOW);
  }

  public static void drawCell(Graphics2D g2d, int x, int y, int width, int height, Cell cell, int time) {
    if (cell.getPredictiveState(time) && cell.getActiveState(time)) {
      drawCell(g2d, x, y, width, height, PREDICTED_COLOR, ACTIVE_COLOR);
    } else if (cell.getPredictiveState(time)) {
      drawCell(g2d, x, y, width, height, PREDICTED_COLOR, PREDICTED_COLOR);
    } else if (cell.getActiveState(time)) {
      drawCell(g2d, x, y, width, height, ACTIVE_COLOR, ACTIVE_COLOR);
    } else {
      drawCell(g2d, x, y, width, height, BACKGROUND_COLOR, ACTIVE_COLOR);
    }
    if (cell.getLearnState(time)) {
      //third of cell width
      int newWidth = width / 3;
      g2d.setColor(LEARNING_COLOR);
      g2d.fillRect(x + newWidth + 1, y + newWidth + 1, newWidth, newWidth);
    }
  }

  private static void drawCell(Graphics2D g2d, int x, int y, int width, int height, Color fillColor, Color borderColor) {
    g2d.setColor(fillColor);
    g2d.fillOval(x, y, width, height);
    g2d.setColor(borderColor);
    g2d.drawOval(x, y, width, height);
  }

  public abstract Cell getCell(int index);

  public void setTime(int time) {
    this.time = time;
    repaint();
  }

}
