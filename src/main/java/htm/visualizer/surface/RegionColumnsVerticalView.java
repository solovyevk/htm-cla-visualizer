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
import htm.utils.MathUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.List;

public class RegionColumnsVerticalView extends CellSurface {
  private int selectedCellIndex = -1;
  private static final Log LOG = LogFactory.getLog(RegionColumnsVerticalView.class);
  protected List<Column> columns;

  public RegionColumnsVerticalView(Region region) {
    super(1, Column.CELLS_PER_COLUMN, region);
    this.updateColumns();
   /* this.addElementMouseEnterListener(new ElementMouseEnterListener() {
      @Override
      public void onElementMouseEnter(ElementMouseEnterEvent e) {
        setSelectedCellIndex(e.getIndex());
      }
    });*/
  }

  public void updateColumns() {
    selectedCellIndex = -1;
    columns = region.getActiveColumns();
    this.dimension.width = columns.size();
  }

  @Override protected int getElementSpaceAllocation() {
    Dimension size = this.getSize();
    double result = MathUtils.findMin((size.getHeight() - 50) / dimension.height,
                                      (size.getWidth() - 2) / dimension.width);
    return result < 1 ? 1 : (int)result - 1;
  }

  @Override
  protected void doDrawing(Graphics2D g2d) {
    super.doDrawing(g2d);
    //Draw column index
    for (int i = 0; i < columns.size(); i++) {
      Rectangle area = getElementAreaByIndex(i);
      int strX = area.x + area.width / 2 - SPACE_BETWEEN_ELEMENTS;
      int strY = area.y - SPACE_BETWEEN_ELEMENTS;
      LOG.debug("Draw Column Index at:" + strX + ", " + strY);
      g2d.setColor(ACTIVE_COLOR);
      g2d.drawString(columns.get(i).getIndex() + "", strX, strY);
    }
    //Draw selection
    if (selectedCellIndex != -1) {
      g2d.setColor(Color.RED);
      Rectangle aroundRec = getElementAreaWithScale(selectedCellIndex, 1 / (Math.PI / 4) * 0.95);
      g2d.drawOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
      Column column = getCell(selectedCellIndex).getBelongsToColumn();
      List<Cell> columnCells = column.getCells();
      for (Cell columnCell : columnCells) {
        int cellInx = indexOf(columnCell);
        if(cellInx != selectedCellIndex){
          aroundRec = getElementAreaWithScale(cellInx, 1 / (Math.PI / 4) *  0.95);
          g2d.setColor(Color.ORANGE);
          g2d.drawOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
        }
      }
    }
  }

  @Override
  public Cell getCell(int index) {
    int cellIndex = index / dimension.width, columnIndex = index % dimension.width;
    return columns.get(columnIndex).getCellByIndex(cellIndex);
  }

  public int indexOf(Cell cell) {
    int index = 0;
    for (int y = 0; y < dimension.height; y++) {
      for (int x = 0; x < dimension.width; x++) {
        if (cell == getCell(index)) {
          return index;
        }
        index++;
      }
    }
    return -1;
  }

  public void setSelectedCellIndex(int selectedCellIndex) {
    this.selectedCellIndex = this.selectedCellIndex != selectedCellIndex ? selectedCellIndex : -1;
           repaint();
  }

  public void setSelectedCell(Cell cell) {
     setSelectedCellIndex(indexOf(cell));
   }
}
