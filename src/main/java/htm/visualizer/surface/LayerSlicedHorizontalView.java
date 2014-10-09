/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.surface;

import htm.model.*;
import htm.utils.UIUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LayerSlicedHorizontalView extends JPanel {
  private static final Log LOG = LogFactory.getLog(LayerSlicedHorizontalView.class);
  private List<ColumnCellsByIndexSurface> layers = new ArrayList<ColumnCellsByIndexSurface>();
  private CellPosition clickedOnCellPosition = null;
  private CellPosition selectedSynapseCellPosition = null;
  private List<CellPosition> selectedSegmentSynapsesCellPositionList = new ArrayList<CellPosition>();


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


  public LayerSlicedHorizontalView(Layer region) {
    super(new GridLayout(0, 1));
    for (int i = 0; i < region.getCellsInColumn(); i++) {
      final ColumnCellsByIndexSurface cellLayer = new ColumnCellsByIndexSurface(this, region, i);
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

  public void setClickedOnCell(Cell clickedOnCell) {
    this.setClickedOnCellPosition(new CellPosition(clickedOnCell.getOwner().getIndex(),
                                                   clickedOnCell.getCellIndex()));
  }

  public void setClickedOnCellPosition(CellPosition clickedOnCellPosition) {
    this.clickedOnCellPosition = clickedOnCellPosition.equals(
            this.clickedOnCellPosition) ? null : clickedOnCellPosition;
    this.selectedSynapseCellPosition = null;
    selectedSegmentSynapsesCellPositionList.clear();
    repaint();
  }


  public CellPosition getSelectedSynapseCellPosition() {
    return selectedSynapseCellPosition;
  }


  public void setSelectedSynapseCellPosition(CellPosition selectedSynapseCellPosition) {
    this.selectedSynapseCellPosition = selectedSynapseCellPosition;
  }

  public void setSelectedSynapse(Synapse.DistalSynapse selectedSynapse) {
    CellPosition newSelectedSynapseCellPosition = new CellPosition(
            selectedSynapse.getFromCell().getOwner().getIndex(),
            selectedSynapse.getFromCell().getCellIndex());
    this.selectedSynapseCellPosition = this.selectedSynapseCellPosition == newSelectedSynapseCellPosition ? null : newSelectedSynapseCellPosition;
  }

  public void setSelectedSegment(DistalDendriteSegment selectedSegment) {
    selectedSegmentSynapsesCellPositionList.clear();
    if (selectedSegment != null) {
      for (Synapse.DistalSynapse distalSynapse : selectedSegment.getElementsList()) {
        selectedSegmentSynapsesCellPositionList.add(new CellPosition(
                distalSynapse.getFromCell().getOwner().getIndex(),
                distalSynapse.getFromCell().getCellIndex()));
      }
    }
  }

  public List<CellPosition> getSelectedSegmentSynapsesCellPositionList() {
    return selectedSegmentSynapsesCellPositionList;
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

  public static class ColumnCellsByIndexSurface extends CellSurface {
    private static final Log LOG = LogFactory.getLog(ColumnCellsByIndexSurface.class);


    public ColumnCellsByIndexSurface(LayerSlicedHorizontalView view, Layer region, int sliceIndex) {
      super(region.getDimension().width, region.getDimension().height, region);
      this.parentView = view;
      this.layerIndex = sliceIndex;
      this.addElementMouseEnterListener(new ElementMouseEnterListener() {
        @Override
        public void onElementMouseEnter(ElementMouseEnterEvent e) {
          CellPosition clickedOnPosition = new CellPosition(e.getIndex(), layerIndex);
          parentView.setClickedOnCellPosition(clickedOnPosition);
        }
      });
    }

    @Override
    public Cell getCell(int columnIndex) {
      return region.getElementByIndex(columnIndex).getElementByIndex(layerIndex);
    }


    private LayerSlicedHorizontalView parentView;
    private final int layerIndex;


    private CellPosition getClickedOnCellPosition() {
      return parentView.getClickedOnCellPosition();
    }

    private CellPosition getSelectedSynapseCellPosition() {
      return parentView.getSelectedSynapseCellPosition();
    }

    private List<CellPosition> getSelectedSegmentSynapsesCellPositionList() {
      return parentView.getSelectedSegmentSynapsesCellPositionList();
    }

 /*   private void repaintAll() {
      for (ColumnCellsByIndexSurface layer : parentView.getLayers()) {
        layer.repaint();
      }
    }

    public void setSelectedSynapseColumnIndex(int selectedSynapseInx) {
      parentView.setSelectedSynapseCellPosition(selectedSynapseInx == -1 ? null : new CellPosition(selectedSynapseInx,
                                                                                                   this.layerIndex));
      repaintAll();
    } */


    public void drawNeighbors(int columnIndex, Graphics2D g2d) {
      Cell clickedOnCell = this.getCell(columnIndex);
      List<Column> neighborColumns = getRegion().getAllWithinRadius(clickedOnCell.getOwner().getPosition(),
                                                                    getRegion().getLearningRadius());
      neighborColumns.remove(clickedOnCell.getOwner());
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

    @Override
    protected void doDrawing(Graphics2D g2d) {
      super.doDrawing(g2d);
      //LOG.debug("Draw Sliced Region");
      CellPosition clickedOn = getClickedOnCellPosition(), selectedSynapse = getSelectedSynapseCellPosition();
      List<CellPosition> selectedSegmentSynapsesCellPositionList = getSelectedSegmentSynapsesCellPositionList();
      if (clickedOn != null) {
        Stroke originalStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1.5f));
        if (clickedOn.getCellIndex() == layerIndex) {
          g2d.setColor(Color.RED);
          Rectangle aroundRec = getElementAreaWithScale(clickedOn.getColumnIndex(), 1 / (Math.PI / 4) * 1.05);
          //Rectangle aroundRec = getElementAreaByIndex(clickedOn.getColumnIndex());
          //g2d.drawLine(aroundRec.x  + aroundRec.width/2, aroundRec.y, aroundRec.x + aroundRec.width/2, aroundRec.y + aroundRec.height);
          //g2d.drawLine(aroundRec.x, aroundRec.y + aroundRec.height/2, aroundRec.x + aroundRec.width, aroundRec.y + aroundRec.height/2);
          g2d.drawOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
        } else {
          Rectangle aroundRec = getElementAreaWithScale(clickedOn.getColumnIndex(), 1 / (Math.PI / 4) * 1.05);
          g2d.setColor(Color.ORANGE);
          g2d.drawOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
        }
        g2d.setStroke(originalStroke);
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
      for (CellPosition cellPosition : selectedSegmentSynapsesCellPositionList) {
        if (cellPosition.getCellIndex() == this.layerIndex) {
          Color originalColor = g2d.getColor();
          g2d.setColor(Color.DARK_GRAY);
          Rectangle aroundRec = getElementAreaByIndex(cellPosition.getColumnIndex());
          g2d.drawRect(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
          g2d.setColor(originalColor);
        }
      }
    }

  }
}
