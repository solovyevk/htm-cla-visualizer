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

  public static void drawCell(Graphics2D g2d, Rectangle rect, Cell cell, int time) {
    drawCell(g2d, rect.x, rect.y, rect.width, rect.height, cell, time);
  }

  public static void drawCell(Graphics2D g2d, int x, int y, int width, int height, Cell cell, int time) {
    if (cell.getPredictiveState(time) && cell.getActiveState(time)) {
      drawCell(g2d, x, y, width, height, PREDICTED_COLOR, ACTIVE_COLOR);
    } else if (cell.getPredictiveState(time)) {
      drawCell(g2d, x, y, width, height, PREDICTED_COLOR);
    } else if (cell.getActiveState(time)) {
      drawCell(g2d, x, y, width, height, ACTIVE_COLOR);
    } else {
      drawCell(g2d, x, y, width, height);
    }
    if (cell.getLearnState(time)) {
      //third of cell width
      int newWidth = width / 3;
      g2d.setColor(LEARNING_COLOR);
      g2d.fillRect(x + newWidth + 1, y + newWidth + 1, newWidth, newWidth);
    }
  }

  private static void drawCell(Graphics2D g2d, int x, int y, int width, int height, Color... stateColors) {
    int cellStates = stateColors.length;
    if (cellStates > 0) {
      int startAngle = 0, arcAngle = 360 / cellStates;
      for (Color stateColor : stateColors) {
        g2d.setColor(stateColor);
        g2d.fillArc(x, y, width, height, startAngle, arcAngle);
        startAngle = startAngle + arcAngle;
      }
    }
    // g2d.setColor(fillColor);
    //  g2d.fillOval(x, y, width, height);
    g2d.setColor(Color.BLACK);
    g2d.drawOval(x, y, width, height);
  }

  public abstract Cell getCell(int index);

  public void setTime(int time) {
    this.time = time;
    repaint();
  }

}
