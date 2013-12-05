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

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegionSlicedView extends JPanel {
  private final Region region;
  private List<ColumnCellsByIndexSurface> layers = new ArrayList<ColumnCellsByIndexSurface>();
  private CellPosition clickedOnCellPosition = null;
  private CellPosition selectedSynapseCellPosition = null;


  public List<ColumnCellsByIndexSurface> getLayers() {
    return Collections.unmodifiableList(layers);
  }

  public ColumnCellsByIndexSurface getLayer(int layerInx) {
    return layers.get(layerInx);
  }

  public void addElementMouseEnterListener(BaseSurface.ElementMouseEnterListener listener) {
    for (BaseSurface layer : layers) {
      layer.addElementMouseEnterListener(listener);
    }
  }


  public RegionSlicedView(Region region) {
    super(new GridLayout(0, 1));
    this.region = region;
    for (int i = 0; i < Column.CELLS_PER_COLUMN; i++) {
      final ColumnCellsByIndexSurface cellLayer = new ColumnCellsByIndexSurface(this, i);
      layers.add(i, cellLayer);
      cellLayer.setBorder(UIUtils.LIGHT_GRAY_BORDER);
      this.add(new Container() {
        private Container init(String caption) {
          this.setLayout(new BorderLayout());
          this.add(new JLabel(caption), BorderLayout.NORTH);
          this.add(cellLayer, BorderLayout.CENTER);
          return this;
        }
      }.init("Layer #" + i));
    }
  }

  public CellPosition getClickedOnCellPosition() {
    return clickedOnCellPosition;
  }

  public void setClickedOnCellPosition(CellPosition clickedOnCellPosition) {
    this.clickedOnCellPosition = clickedOnCellPosition;
  }

  public CellPosition getSelectedSynapseCellPosition() {
    return selectedSynapseCellPosition;
  }

  public void setSelectedSynapseCellPosition(CellPosition selectedSynapseCellPosition) {
    this.selectedSynapseCellPosition = selectedSynapseCellPosition;
  }

  public static class CellPosition {
    private final int columnIndex;
    private final int cellIndex;

    private CellPosition(Integer columnIndex, Integer cellIndex) {
      this.columnIndex = columnIndex;
      this.cellIndex = cellIndex;
    }

    public Integer getColumnIndex() {
      return columnIndex;
    }

    public Integer getCellIndex() {
      return cellIndex;
    }

    public boolean equals(Object obj) {
      if (obj instanceof CellPosition) {
        CellPosition cp = (CellPosition)obj;
        return (columnIndex == cp.columnIndex) && (cellIndex == cp.cellIndex);
      }
      return super.equals(obj);
    }
  }


  public Region getRegion() {
    return region;
  }

  public static class ColumnCellsByIndexSurface extends BaseSurface.CircleElementsSurface {
    private static final Log LOG = LogFactory.getLog(ColumnCellsByIndexSurface.class);

    public static final Color DEFAULT_PREDICTED_COLOR = Color.BLUE;
    public static final Color DEFAULT_LEARNING_COLOR = Color.RED;

    private RegionSlicedView parentView;
    private final int layerIndex;


    protected final Cell[] cells;


    protected Color predictedColor = DEFAULT_PREDICTED_COLOR;
    protected Color learningColor = DEFAULT_LEARNING_COLOR;

    private int time = Cell.NOW;

    private Region getRegion() {
      return parentView.getRegion();
    }

    private CellPosition getClickedOnCellPosition() {
      return parentView.getClickedOnCellPosition();
    }

    private CellPosition getSelectedSynapseCellPosition(){
      return parentView.getSelectedSynapseCellPosition();
    }

    public ColumnCellsByIndexSurface(RegionSlicedView view, int sliceIndex) {
      super(view.getRegion().getDimension().width, view.getRegion().getDimension().height);
      this.parentView = view;
      Column[] columns = getRegion().getColumns();
      cells = new Cell[columns.length];
      for (int j = 0; j < columns.length; j++) {
        cells[j] = columns[j].getCellByIndex(sliceIndex);
      }
      this.layerIndex = sliceIndex;
      this.addElementMouseEnterListener(new ElementMouseEnterListener() {
        @Override
        public void onElementMouseEnter(ElementMouseEnterEvent e) {
          CellPosition clickedOnPosition = new CellPosition(e.getIndex(), layerIndex);
          parentView.setClickedOnCellPosition(clickedOnPosition.equals(parentView.getClickedOnCellPosition()) ? null : clickedOnPosition);
          //clickedOnCellPosition = clickedOnCellInx == e.getIndex() ? -1 : e.getIndex();
          //clickedOnLayerInx = layerIndex;
         // selectedSynapseInx = -1;
          parentView.setSelectedSynapseCellPosition(null);
          repaintAll();
        }
      });
    }

    private void repaintAll() {
      for (ColumnCellsByIndexSurface layer : parentView.getLayers()) {
        layer.repaint();
      }
    }

    public void setSelectedSynapseColumnIndex(int selectedSynapseInx) {
      parentView.setSelectedSynapseCellPosition(selectedSynapseInx == -1 ? null : new CellPosition(selectedSynapseInx, this.layerIndex));
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
      List<Column> neighborColumns = getRegion().getAllWithinRadius(clickedOnCell.getBelongsToColumn().getPosition(),
                                                                    getRegion().getLearningRadius());
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
      CellPosition clickedOn = getClickedOnCellPosition(), selectedSynapse = getSelectedSynapseCellPosition();
      if (clickedOn != null && clickedOn.getCellIndex() == layerIndex) {
        Rectangle aroundRec = getElementAreaWithScale(clickedOn.getColumnIndex(), 1 / (Math.PI / 4));
        g2d.setColor(Color.ORANGE);
        g2d.drawOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
      }
      if (clickedOn != null) {
        drawNeighbors(clickedOn.getColumnIndex(), g2d);
      }
      if (selectedSynapse != null && selectedSynapse.getCellIndex() == this.layerIndex) {
        Composite original = g2d.getComposite();
        Rectangle aroundRec = getElementAreaWithScale(selectedSynapse.getColumnIndex(), 1 / (Math.PI / 4) * 1.5);
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
}
