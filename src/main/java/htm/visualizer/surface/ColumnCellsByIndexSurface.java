/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.surface;


import htm.model.Cell;

import java.awt.*;

public class ColumnCellsByIndexSurface extends BaseSurface.CircleElementsSurface {
  public static final Color DEFAULT_PREDICTED_COLOR = Color.BLUE;
  public static final Color DEFAULT_LEARNING_COLOR = Color.RED;

  protected final Cell[] cells;

  protected Color predictedColor = DEFAULT_PREDICTED_COLOR;
  protected Color learningColor = DEFAULT_LEARNING_COLOR;

  private int time = Cell.NOW;

  public ColumnCellsByIndexSurface(int xSize, int ySize, Cell[] layer) {
    super(xSize, ySize);
    this.cells = layer;
  }


  @Override
  protected void drawElement(Graphics2D g2d, int columnIndex, int x, int y, int width, int height) {
    Cell currentCell = getCell(columnIndex);
    if(currentCell.getPredictiveState(time) && currentCell.getActiveState(time)){
      drawCell(g2d, x, y, width, height, predictedColor, activeColor);
    } else if(currentCell.getPredictiveState(time)) {
      drawCell(g2d, x, y, width, height, predictedColor, predictedColor);
    } else if(currentCell.getActiveState(time)){
      drawCell(g2d, x, y, width, height, activeColor, activeColor);
    } else {
      drawCell(g2d, x, y, width, height, getBackground(), activeColor);
    }
    if(currentCell.getLearnState(time)){
      //third of cell width
      int newWidth =  width/3;
      g2d.setColor(learningColor);
      g2d.fillRect(x + newWidth + 1, y + newWidth + 1, newWidth, newWidth);
    }
  }

  private void drawCell(Graphics2D g2d, int x, int y, int width, int height, Color fillColor, Color borderColor) {
    g2d.setColor(fillColor);
    g2d.fillOval(x, y, width, height);
    g2d.setColor(borderColor);
    g2d.drawOval(x, y, width, height);
  }


  public Cell getCell(int columnIndex){
    return cells[columnIndex];
  }

  public void setTime(int time) {
    this.time = time;
    repaint();
  }

}
