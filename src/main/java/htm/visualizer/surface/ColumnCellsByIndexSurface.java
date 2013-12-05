/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.surface;


import htm.model.Cell;
import htm.model.Column;
import htm.model.Region;
import htm.utils.UIUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.ArrayList;


public class ColumnCellsByIndexSurface extends BaseSurface.CircleElementsSurface {
  private static final Log LOG = LogFactory.getLog(ColumnCellsByIndexSurface.class);

  public static final Color DEFAULT_PREDICTED_COLOR = Color.BLUE;
  public static final Color DEFAULT_LEARNING_COLOR = Color.RED;
  //just to see selected cell
  private static int clickedOnCellInx = -1;
  private static int clickedOnLayerInx = -1;
  private static int selectedSynapseInx = -1;
  private static java.util.List<ColumnCellsByIndexSurface> allLayers = new ArrayList<ColumnCellsByIndexSurface>();
  private final int layerIndex;
  private final Region region;


  protected final Cell[] cells;


  protected Color predictedColor = DEFAULT_PREDICTED_COLOR;
  protected Color learningColor = DEFAULT_LEARNING_COLOR;

  private int time = Cell.NOW;

  public ColumnCellsByIndexSurface(Region region, int sliceIndex) {
    super(region.getDimension().width, region.getDimension().height);
    this.region = region;
    Column[] columns = region.getColumns();
    cells = new Cell[columns.length];
    for (int j = 0; j < columns.length; j++) {
      cells[j] = columns[j].getCellByIndex(sliceIndex);
    }
    allLayers.add(this);
    this.layerIndex = sliceIndex;
    this.addElementMouseEnterListener(new ElementMouseEnterListener() {
      @Override
      public void onElementMouseEnter(ElementMouseEnterEvent e) {
        clickedOnCellInx = clickedOnCellInx == e.getIndex() ? -1 : e.getIndex();
        clickedOnLayerInx = layerIndex;
        selectedSynapseInx = -1;
        repaintAll();
      }
    });
  }

  private void repaintAll() {
    for (ColumnCellsByIndexSurface layer : allLayers) {
      layer.repaint();
    }
  }

  public void setSelectedSynapseInx(int selectedSynapseInx) {
    ColumnCellsByIndexSurface.selectedSynapseInx = selectedSynapseInx;
    repaintAll();
  }


  @Override
  protected void drawElement(Graphics2D g2d, int columnIndex, int x, int y, int width, int height) {
    Cell currentCell = getCell(columnIndex);
    if (currentCell.getPredictiveState(time) && currentCell.getActiveState(time)) {
      drawCell(g2d, x, y, width, height, predictedColor, activeColor);
    } else if (currentCell.getPredictiveState(time)) {
      drawCell(g2d, x, y, width, height, predictedColor, predictedColor);
    } else if (currentCell.getActiveState(time)) {
      drawCell(g2d, x, y, width, height, activeColor, activeColor);
    } else {
      drawCell(g2d, x, y, width, height, getBackground(), activeColor);
    }
    if (currentCell.getLearnState(time)) {
      //third of cell width
      int newWidth = width / 3;
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

  public void drawNeighbors(int columnIndex, Graphics2D g2d) {
    Cell clickedOnCell = this.getCell(columnIndex);
    java.util.List<Column> neighborColumns = region.getAllWithinRadius(clickedOnCell.getBelongsToColumn().getPosition(),
                                                                       region.getLearningRadius());
    neighborColumns.remove(clickedOnCell.getBelongsToColumn());
    g2d.setColor(Color.LIGHT_GRAY);
    Composite original = g2d.getComposite();
    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                0.4f));
    for (Column column : neighborColumns) {
      Rectangle columnRec = this.getElementArea(column.getPosition());
      g2d.fillOval(columnRec.x, columnRec.y, columnRec.width, columnRec.height);
    }
    g2d.setComposite(original);

  }

  @Override protected void doDrawing(Graphics2D g2d) {
    super.doDrawing(g2d);
    if (clickedOnCellInx != -1 && clickedOnLayerInx == layerIndex) {
      Rectangle aroundRec = getElementAreaWithScale(clickedOnCellInx, 1 / (Math.PI / 4));
      g2d.setColor(Color.ORANGE);
      g2d.drawOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
    }
    if (clickedOnCellInx != -1) {
      drawNeighbors(clickedOnCellInx, g2d);
    }
    if (selectedSynapseInx != -1) {
      Composite original = g2d.getComposite();
      Rectangle aroundRec = getElementAreaWithScale(selectedSynapseInx, 1 / (Math.PI / 4) * 1.5);
      g2d.setColor(UIUtils.LIGHT_BLUE);
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                  0.5f));
      g2d.fillOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
      g2d.setComposite(original);
    }
  }

  public Cell getCell(int columnIndex) {
    return cells[columnIndex];
  }

  public void setTime(int time) {
    this.time = time;
    repaint();
  }

}
