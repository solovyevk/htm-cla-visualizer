/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer;

import htm.model.Cell;
import htm.model.DistalDendriteSegment;
import htm.model.Region;
import htm.model.Synapse;
import htm.utils.UIUtils;
import htm.visualizer.surface.CellSurface;
import htm.visualizer.surface.RegionColumnsVerticalView;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.LinkedHashMap;
import java.util.Map;

public class TemporalInfo extends JPanel {
  private Cell currentCell;
  private JTable distalDendriteSegmentsTable;
  private JTable segmentDistalSynapsesTable;
  private JTable distalDendriteSegmentUpdatesTable;
  private JTable segmentUpdateDistalSynapsesTable;
  private RegionColumnsVerticalView regionColumnsVerticalView;

  public TemporalInfo(Region region) {
    this.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.NORTH;
    c.gridx = 0;
    c.weighty = 1.0;
    c.weightx = 1.0;

    distalDendriteSegmentsTable = initDistalDendriteSegmentsTable();
    segmentDistalSynapsesTable = initSegmentDistalSynapsesTable();
    distalDendriteSegmentUpdatesTable = initDistalDendriteSegmentUpdatesTable();
    segmentUpdateDistalSynapsesTable = initSegmentUpdateDistalSynapsesTable();
    //listeners
    distalDendriteSegmentsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override public void valueChanged(ListSelectionEvent e) {
        BaseSegmentDistalSynapsesModel synapsesModel = (BaseSegmentDistalSynapsesModel)segmentDistalSynapsesTable.getModel();
        int rowViewInx = distalDendriteSegmentsTable.getSelectedRow();
        if (rowViewInx == -1) {
          synapsesModel.setSegment(null);
        } else {
          int rowColumnModelInx = distalDendriteSegmentsTable.convertRowIndexToModel(rowViewInx);
          DistalDendriteSegment segment = ((DistalDendriteSegmentsModel)distalDendriteSegmentsTable.getModel()).getSegment(
                  rowColumnModelInx);
          synapsesModel.setSegment(segment);
        }
      }
    });
    distalDendriteSegmentUpdatesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override public void valueChanged(ListSelectionEvent e) {
        BaseSegmentDistalSynapsesModel synapsesModel = (BaseSegmentDistalSynapsesModel)segmentUpdateDistalSynapsesTable.getModel();
        int rowViewInx = distalDendriteSegmentUpdatesTable.getSelectedRow();
        if (rowViewInx == -1) {
          synapsesModel.setSegment(null);
        } else {
          int rowColumnModelInx = distalDendriteSegmentUpdatesTable.convertRowIndexToModel(rowViewInx);
          DistalDendriteSegment segmentUpdate = ((DistalDendriteSegmentUpdatesModel)distalDendriteSegmentUpdatesTable.getModel()).getSegment(
                  rowColumnModelInx);
          synapsesModel.setSegment(segmentUpdate);
        }
      }
    });

    JPanel left = new CellAttributesInfo();
    left.setBackground(Color.WHITE);
    left.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                    "Cell Properties",
                                                    TitledBorder.CENTER,
                                                    TitledBorder.TOP));
    this.add(left, c);
    c.gridx = 1;
    c.weightx = 1.2;

    JPanel center_left = (new JPanel() {
      private JPanel init() {
        this.setLayout(new GridLayout(2, 0, 5, 5));
        add(new JScrollPane(distalDendriteSegmentsTable));
        distalDendriteSegmentsTable.setFillsViewportHeight(true);
        add(new JScrollPane(segmentDistalSynapsesTable));
        segmentDistalSynapsesTable.setFillsViewportHeight(true);
        return this;
      }
    }.init());
    center_left.setBackground(Color.WHITE);
    center_left.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                           "Segments & Synapses",
                                                           TitledBorder.CENTER,
                                                           TitledBorder.TOP));
    this.add(center_left, c);
    c.gridx = 2;
    c.weightx = 1.2;
    JPanel center_right = (new JPanel() {
      private JPanel init() {
        this.setLayout(new GridLayout(2, 0, 5, 5));
        add(new JScrollPane(distalDendriteSegmentUpdatesTable));
        distalDendriteSegmentUpdatesTable.setFillsViewportHeight(true);
        add(new JScrollPane(segmentUpdateDistalSynapsesTable));
        segmentUpdateDistalSynapsesTable.setFillsViewportHeight(true);
        return this;
      }
    }.init());
    center_right.setBackground(Color.WHITE);
    center_right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                            "Updates & Synapses",
                                                            TitledBorder.CENTER,
                                                            TitledBorder.TOP));
    this.add(center_right, c);
    c.gridx = 3;
    c.weightx = 2.0;
    regionColumnsVerticalView = new RegionColumnsVerticalView(region);
    final JScrollPane right = new JScrollPane(regionColumnsVerticalView);
    right.setBackground(Color.WHITE);
    right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                     "Active Columns",
                                                     TitledBorder.CENTER,
                                                     TitledBorder.TOP));
    this.add(right, c);


  }

  public void setCurrentCell(Cell currentCell) {
    this.currentCell = this.currentCell != currentCell ? currentCell : null;
    ((DistalDendriteSegmentsModel)distalDendriteSegmentsTable.getModel()).setCell(this.currentCell);
    ((DistalDendriteSegmentUpdatesModel)distalDendriteSegmentUpdatesTable.getModel()).setCell(this.currentCell);
    regionColumnsVerticalView.setSelectedCell(currentCell);
    this.repaint();
  }

  private JTable initDistalDendriteSegmentsTable() {
    final TemporalInfo that = this;
    final JTable table = new JTable(new DistalDendriteSegmentsModel());
    table.setAutoCreateRowSorter(true);
    //Need this to be able to delete segment on demand for research purpose
    JPopupMenu popup = new JPopupMenu();
    popup.add(new AbstractAction("Delete segment", UIUtils.INSTANCE.createImageIcon(
            "/images/delete.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        DistalDendriteSegment selected = ((SegmentsModel)table.getModel()).getSegment(table.getSelectedRow());
        that.currentCell.deleteSegment(selected);
        ((SegmentsModel)table.getModel()).fireTableDataChanged();
      }
    });
    popup.add(new AbstractAction("Delete All Segment", UIUtils.INSTANCE.createImageIcon(
            "/images/delete.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        that.currentCell.deleteAllSegment();
        ((SegmentsModel)table.getModel()).fireTableDataChanged();
      }
    });
    table.setComponentPopupMenu(popup);
    return table;
  }

  private JTable initSegmentDistalSynapsesTable() {
    JTable table = new JTable(new SegmentDistalSynapsesModel());
    return getSynapsesTable(table);
  }

  private JTable initSegmentUpdateDistalSynapsesTable() {
    JTable table = new JTable(new SegmentUpdateDistalSynapsesModel());
    table.getColumnModel().getColumn(4).setPreferredWidth(20);
    return getSynapsesTable(table);
  }

  private JTable getSynapsesTable(JTable table) {
    table.setAutoCreateRowSorter(true);
    table.getColumnModel().getColumn(0).setCellRenderer(new UIUtils.PermanenceRenderer());
    table.getColumnModel().getColumn(1).setPreferredWidth(30);
    table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
      private Cell cell;

      @Override public void paint(Graphics g) {
        super.paint(g);
        Dimension size = this.getSize();
        Rectangle insideRec = new Rectangle(size.width / 2 - (size.height - 4) / 2, 2, size.height - 4,
                                            size.height - 4);
        Graphics2D g2d = (Graphics2D)g;
        CellSurface.drawCell(g2d, insideRec, cell, Cell.NOW);
        g2d.setColor(Color.BLACK);
      }

      @Override
      protected void setValue(Object value) {
        cell = (Cell)value;
      }
    });
    table.getColumnModel().getColumn(2).setPreferredWidth(30);
    table.getColumnModel().getColumn(3).setCellRenderer(new UIUtils.PositionRenderer());
    table.getColumnModel().getColumn(4).setPreferredWidth(50);
    table.setAutoCreateRowSorter(true);
    return table;
  }

  private JTable initDistalDendriteSegmentUpdatesTable() {
    JTable table = new JTable(new DistalDendriteSegmentUpdatesModel()) {
      //need to override to avoid exception in sorter
      @Override
      public Object getValueAt(int row, int column) {
        if (row > getModel().getRowCount() - 1) {
          return null;
        } else {
          return super.getValueAt(row, column);
        }
      }
    };
    table.setAutoCreateRowSorter(true);
    return table;
  }

  public JTable getSegmentDistalSynapsesTable() {
    return segmentDistalSynapsesTable;
  }

  public JTable getSegmentUpdateDistalSynapsesTable() {
    return segmentUpdateDistalSynapsesTable;
  }

  public JTable getDistalDendriteSegmentsTable() {
    return distalDendriteSegmentsTable;
  }

  public JTable getDistalDendriteSegmentUpdatesTable() {
    return distalDendriteSegmentUpdatesTable;
  }

  public RegionColumnsVerticalView getRegionColumnsVerticalView() {
    return regionColumnsVerticalView;
  }


  private class CellAttributesInfo extends UIUtils.TextColumnInfo {
    {
      startX = 90;
    }

    @Override public void paint(Graphics g) {
      super.paint(g);
      if (currentCell != null) {
        Graphics2D g2 = (Graphics2D)g;
        FontRenderContext frc = g2.getFontRenderContext();
        float drawPosY = finishParagraphY + 2;
        Font timeFont = new Font("Helvetica", Font.BOLD, 11);
        int x = startX + 5;
        for (int i = 0; i < Cell.TIME_STEPS; i++) {
          TextLayout timeLayout = new TextLayout("t - " + i + ": ", timeFont, frc);
          CellSurface.drawCell(g2, x, (int)drawPosY + 1, 11, 11, currentCell, i);
          float drawPosX;
          drawPosX = (float)x - timeLayout.getAdvance();
          drawPosY += timeLayout.getAscent();
          g2.setColor(Color.BLACK);
          timeLayout.draw(g2, drawPosX, drawPosY);
          drawPosY += timeLayout.getDescent() + timeLayout.getLeading();
        }
      }
    }

    @Override
    protected Map<String, String> getAttributeMap() {
      Cell cell = currentCell;
      Map<String, String> result = new LinkedHashMap<String, String>();
      if (cell != null) {
        result.put("Column Inx", cell.getBelongsToColumn().getIndex() + "");
        result.put("Cell Inx", cell.getCellIndex() + "");
        result.put("Position",
                   "X:" + (cell.getBelongsToColumn().getPosition().x) + ", Y:" + cell.getBelongsToColumn().getPosition().y);
        /*result.put("Active", cell.getActiveState(Cell.NOW) ? "Yes" : "No");
        result.put("Predictive", cell.getPredictiveState(Cell.NOW) ? "Yes" : "No");
        result.put("Learn", cell.getLearnState(Cell.NOW) ? "Yes" : "No"); */
        result.put("Segments", cell.getSegments().size() + "");
        result.put("Updates", cell.getSegmentUpdates().size() + "");
      }
      return result;
    }
  }


  abstract class SegmentsModel extends AbstractTableModel {
    protected java.util.List<? extends DistalDendriteSegment> segments = null;

    private Cell.SegmentsChangeEventListener segmentsUpdatesChangeEventListener = new Cell.SegmentsChangeEventListener() {
      @Override public void onSegmentsChange(Cell.SegmentsChangeEvent e) {
        segmentsChange();
      }

      @Override public void onUpdatesChange(Cell.SegmentsChangeEvent e) {
        updatesChange();
      }
    };

    protected void segmentsChange() {
    }

    protected void updatesChange() {
    }

    protected String[] columnNames = {
            "Seg",
            "Seq",
            "Act",
            "Learn",
            "By",
            "St",
            "Syn"
    };

    public void setCell(Cell cell) {
      this.fireTableDataChanged();
      if (cell != null) {
        cell.addSegmentsChangeListener(segmentsUpdatesChangeEventListener);
      }
    }

    @Override public int getRowCount() {
      return segments == null ? 0 : segments.size();
    }

    @Override public int getColumnCount() {
      return columnNames.length;
    }

    @Override
    public String getColumnName(int col) {
      return columnNames[col];
    }

    public DistalDendriteSegment getSegment(int rowIndex) {
      DistalDendriteSegment segment = null;
      if (segments != null) {
        segment = segments.get(rowIndex);
      }
      return segment;
    }

  }

  class DistalDendriteSegmentsModel extends SegmentsModel {

    @Override
    public void setCell(Cell cell) {
      segments = cell != null ? cell.getSegments() : null;
      super.setCell(cell);
    }

    @Override protected void segmentsChange() {
      // this.fireTableDataChanged();  -want to keep selected list
      this.fireTableDataChanged();
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex) {
      Object value = null;
      if (segments != null) {
        DistalDendriteSegment row = segments.get(rowIndex);
        if (row != null) {
          switch (columnIndex) {
            case 0:
              value = rowIndex;
              break;
            case 1:
              value = row.isSequenceSegment();
              break;
            case 2:
              value = row.segmentActive(Cell.NOW, Cell.State.ACTIVE);
              break;
            case 3:
              value = row.segmentActive(Cell.NOW, Cell.State.LEARN);
              break;
            case 4:
              value = row.getPredictedBy() == null ? "R" : segments.indexOf(row.getPredictedBy()) + "";
              break;
            case 5:
              value = row.predictedInStep();
              break;
            case 6:
              value = row.size();
              break;
            default:
              value = null;
          }
        }
      }
      return value;
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
      Class result;
      switch (columnIndex) {
        case 0:
          result = Integer.class;
          break;
        case 1:
          result = Boolean.class;
          break;
        case 2:
          result = Boolean.class;
          break;
        case 3:
          result = Boolean.class;
          break;
        case 4:
          result = String.class;
          break;
        case 6:
          result = Integer.class;
          break;
        case 5:
          result = Integer.class;
          break;
        default:
          result = super.getColumnClass(
                  columnIndex);
      }
      return result;
    }
  }

  class DistalDendriteSegmentUpdatesModel extends SegmentsModel {

    {
      columnNames = new String[]{
              "Seg",
              "Seq",
              "Time",
              "St",
              "Syn"
      };
    }

    @Override protected void updatesChange() {
      this.fireTableDataChanged();
    }

    @Override
    public void setCell(Cell cell) {
      segments = cell != null ? cell.getSegmentUpdates() : null;
      super.setCell(cell);
    }


    @Override public int getColumnCount() {
      return columnNames.length;
    }

    @Override
    public String getColumnName(int col) {
      return columnNames[col];
    }


    @Override public Object getValueAt(int rowIndex, int columnIndex) {
      Object value = null;
      if (segments != null && segments.size() > rowIndex) {
        DistalDendriteSegment.Update row = (DistalDendriteSegment.Update)segments.get(rowIndex);
        if (row != null) {
          switch (columnIndex) {
            case 0:
              value = row.getTarget() == null ? "new" : currentCell.getSegments().indexOf(row.getTarget()) + "";
              break;
            case 1:
              value = row.isSequenceSegment();
              break;
            case 2:
              value = row.getTime() == Cell.NOW ? "NOW" : "BEF";
              break;
            case 3:
              value = row.predictedInStep();
              break;
            case 4:
              value = row.size();
              break;
            default:
              value = null;
          }
        }
      }
      return value;
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
      Class result;
      switch (columnIndex) {
        case 0:
          result = String.class;
          break;
        case 1:
          result = Boolean.class;
          break;
        case 2:
          result = String.class;
          break;
        case 3:
          result = Integer.class;
          break;
        case 4:
          result = Integer.class;
          break;
        default:
          result = super.getColumnClass(
                  columnIndex);
      }
      return result;
    }
  }


  abstract class BaseSegmentDistalSynapsesModel extends AbstractTableModel {
    protected java.util.List<Synapse.DistalSynapse> synapses = null;
    protected String[] columnNames = {
            "Perm",
            "Cell",
            "Inx",
            "Position"
    };

    public void setSegment(DistalDendriteSegment segment) {
      synapses = segment != null ? segment : null;
      this.fireTableDataChanged();
    }

    @Override public int getRowCount() {
      return synapses == null ? 0 : synapses.size();
    }

    @Override public int getColumnCount() {
      return columnNames.length;
    }

    @Override
    public String getColumnName(int col) {
      return columnNames[col];
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex) {
      Object value = null;
      if (synapses != null) {
        Synapse.DistalSynapse row = synapses.get(rowIndex);
        if (row != null) {
          switch (columnIndex) {
            case 0:
              value = row.getPermanence();
              break;
            case 1:
              value = row.getFromCell();
              break;
            case 2:
              value = row.getFromCell().getCellIndex();
              break;
            case 3:
              value = new UIUtils.SortablePoint(row.getFromCell().getBelongsToColumn().getPosition());
              break;
            default:
              value = null;
          }
        }
      }
      return value;
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
      Class result;
      switch (columnIndex) {
        case 0:
          result = Double.class;
          break;
        case 1:
          result = Cell.class;
          break;
        case 2:
          result = Integer.class;
          break;
        case 3:
          result = UIUtils.SortablePoint.class;
          break;
        default:
          result = super.getColumnClass(
                  columnIndex);
      }
      return result;
    }

    public Synapse.DistalSynapse getSynapse(int rowIndex) {
      Synapse.DistalSynapse synapse = null;
      if (synapses != null) {
        synapse = synapses.get(rowIndex);
      }
      return synapse;
    }

  }

  class SegmentDistalSynapsesModel extends BaseSegmentDistalSynapsesModel {
    {
      columnNames = new String[]{
              "Perm",
              "Cell",
              "Inx",
              "Position",
              "r"
      };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      Synapse.DistalSynapse row = synapses.get(rowIndex);
      if(row != null){
      if (columnIndex == 4) {
        return row.getPermanenceRangeChangeForActive();
      }
      }
      return super.getValueAt(rowIndex, columnIndex);
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
      if (columnIndex == 4) {
        return Double.class;
      }
      return super.getColumnClass(columnIndex);
    }
  }

  class SegmentUpdateDistalSynapsesModel extends BaseSegmentDistalSynapsesModel {
    {
      columnNames = new String[]{
              "Perm",
              "Cell",
              "Inx",
              "Position",
              "n"
      };
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      Synapse.DistalSynapse row = synapses.get(rowIndex);
      if(row != null){
      if (columnIndex == 4) {
        return row.getSegment() instanceof DistalDendriteSegment.Update;
      }
      }
      return super.getValueAt(rowIndex, columnIndex);
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
      if (columnIndex == 4) {
        return Boolean.class;
      }
      return super.getColumnClass(columnIndex);
    }
  }


}
