/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.surface;

import htm.model.Cell;
import htm.model.Layer;
import htm.utils.UIUtils;

import java.awt.*;

public abstract class CellSurface extends BaseSurface.CircleElementsSurface {
  public static final Color PREDICTED_COLOR = Color.BLUE;
  public static final Color PREDICTED_IN_STEP_COLOR = UIUtils.LIGHT_BLUE;
  public static final Color LEARNING_COLOR = Color.RED;
  private int time = Cell.NOW;
  protected Layer region;

  public CellSurface(int xSize, int ySize, Layer region) {
    super(xSize, ySize);
    this.region = region;
  }

  protected Layer getRegion() {
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
    int predictInStep = cell.getPredictInStepState(time);
    Color predictedColor = predictInStep > 1 ? PREDICTED_IN_STEP_COLOR : PREDICTED_COLOR;
    if (cell.getPredictiveState(time) && cell.getActiveState(time)) {
      UIUtils.drawStatesInCircle(g2d, x, y, width, height, predictedColor, ACTIVE_COLOR);
    } else if (cell.getPredictiveState(time)) {
      UIUtils.drawStatesInCircle(g2d, x, y, width, height, predictedColor);
    } else if (cell.getActiveState(time)) {
      UIUtils.drawStatesInCircle(g2d, x, y, width, height, ACTIVE_COLOR);
    } else {
      UIUtils.drawStatesInCircle(g2d, x, y, width, height);
    }
    if (cell.getLearnState(time)) {
      //third of cell width
      int newWidth = width / 3;
      g2d.setColor(LEARNING_COLOR);
      g2d.fillRect(x + newWidth + 1, y + newWidth + 1, newWidth, newWidth);
    }
    if(predictInStep > 1){
      String predictInStepStr = predictInStep+"";
      UIUtils.drawTextInCenter(g2d, x, y, width, height, predictInStepStr);
    }
  }

  public abstract Cell getCell(int index);

  public void setTime(int time) {
    this.time = time;
    repaint();
  }

}
