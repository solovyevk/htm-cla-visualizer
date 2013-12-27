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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.List;

public class RegionColumnsVerticalView extends CellSurface implements Scrollable {
  private int selectedCellIndex = -1;
  private static final Log LOG = LogFactory.getLog(RegionColumnsVerticalView.class);
  protected List<Column> columns;
  private static final int MIN_CELL_SIZE = 28;
  private int columnIndexHeight = 12;

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
    resizeAndRepaint();
  }

  @Override
  public Dimension getPreferredSize() {
    int elementSpaceAllocation = getElementSpaceAllocation(),
            prefHeight = super.getPreferredSize().height,
            prefWidth = super.getPreferredSize().width,
            columnWidthAllocation = elementSpaceAllocation * dimension.width,
            columnHeightAllocation = elementSpaceAllocation * dimension.height + columnIndexHeight;
    return new Dimension(columnWidthAllocation < prefWidth ? prefWidth : columnWidthAllocation,
                         columnHeightAllocation < prefHeight ? prefHeight : columnHeightAllocation);
  }

  protected void resizeAndRepaint() {
    revalidate();
    repaint();
  }

  @Override
  protected int getElementSpaceAllocation() {
    return Math.max(MIN_CELL_SIZE, (40 - Column.CELLS_PER_COLUMN * 2));

  }

  @Override protected Point getElementStartPoint(int elementSpaceAllocation) {
    int y = super.getElementStartPoint(elementSpaceAllocation).y + columnIndexHeight;
    return new Point(2, y);
  }


  @Override
  protected void doDrawing(Graphics2D g2d) {
    super.doDrawing(g2d);
    //Draw column index
    for (int i = 0; i < columns.size(); i++) {
      Rectangle area = getElementAreaByIndex(i);
      g2d.setColor(ACTIVE_COLOR);
      drawColumnIndex(g2d, columns.get(i).getIndex(), area);
    }
    //Draw selection
    Stroke originalStroke = g2d.getStroke();
    g2d.setStroke(new BasicStroke(2));
    if (selectedCellIndex != -1) {
      g2d.setColor(Color.RED);
      Rectangle aroundRec = getElementAreaWithScale(selectedCellIndex, 1 / (Math.PI / 4) * 0.9);
      g2d.drawOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
      Column column = getCell(selectedCellIndex).getBelongsToColumn();
      List<Cell> columnCells = column.getCells();
      for (Cell columnCell : columnCells) {
        int cellInx = indexOf(columnCell);
        if (cellInx != selectedCellIndex) {
          aroundRec = getElementAreaWithScale(cellInx, 1 / (Math.PI / 4) * 0.9);
          g2d.setColor(Color.ORANGE);
          g2d.drawOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
        }
      }
    }
    g2d.setStroke(originalStroke);
  }

  protected int getIndexCaptionYPosShift(){
    return SPACE_BETWEEN_ELEMENTS;
  }

  protected void drawColumnIndex(Graphics2D g2, int columnIndex, Rectangle firstCellArea) {
    FontRenderContext frc = g2.getFontRenderContext();
    Font indexFont = new Font("Helvetica", Font.BOLD, 12);
    TextLayout indexLayout = new TextLayout(columnIndex + "", indexFont, frc);
    int drawPosX = (int)(firstCellArea.getX() + (firstCellArea.getWidth() - indexLayout.getAdvance()) / 2);
    int drawPosY = firstCellArea.y - getIndexCaptionYPosShift();
    indexLayout.draw(g2, drawPosX, drawPosY);
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

  @Override public Dimension getPreferredScrollableViewportSize() {
    return getParent() instanceof JViewport ? getParent().getSize() : getPreferredSize();
  }

  @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return getElementSpaceAllocation();
  }

  @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return getElementSpaceAllocation();
  }

  @Override public boolean getScrollableTracksViewportWidth() {
    return getParent() instanceof JViewport
           && (getParent().getWidth() > getPreferredSize().width);
  }

  @Override public boolean getScrollableTracksViewportHeight() {
    return getParent() instanceof JViewport
           && (getParent().getHeight() > getPreferredSize().height);
  }

}
