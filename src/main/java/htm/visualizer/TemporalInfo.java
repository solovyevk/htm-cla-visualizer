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
import htm.model.Synapse;
import htm.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class TemporalInfo extends JPanel {
  private Cell currentCell;
  private JTable distalDendriteSegmentsTable;
  private JTable segmentDistalSynapsesTable;

  public TemporalInfo() {
    this.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.NORTH;
    c.gridx = 0;
    c.weighty = 1.0;
    c.weightx = 1.0;
    CellAttributesInfo left = new CellAttributesInfo();
    left.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                    "Cell Properties",
                                                    TitledBorder.CENTER,
                                                    TitledBorder.TOP));
    this.add(left, c);
    c.gridx = 1;
    c.weightx = 2.0;
    //Create the scroll pane and add the table to it.
    distalDendriteSegmentsTable = initDistalDendriteSegmentsTable();
    segmentDistalSynapsesTable = initSegmentDistalSynapsesTable();
    distalDendriteSegmentsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override public void valueChanged(ListSelectionEvent e) {
        SegmentDistalSynapsesModel synapsesModel = (SegmentDistalSynapsesModel)segmentDistalSynapsesTable.getModel();
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

    JPanel center = (new JPanel() {
      private JPanel init() {
        this.setLayout(new GridLayout(2, 0, 5, 5));
        add(new JScrollPane(distalDendriteSegmentsTable));
        add(new JScrollPane(segmentDistalSynapsesTable));
        return this;
      }
    }.init());
    center.setBackground(Color.WHITE);
    center.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                      "Segments & Synapses",
                                                      TitledBorder.CENTER,
                                                      TitledBorder.TOP));
    //Add the scroll pane to this panel.
    this.add(center, c);
  }

  public void setCurrentCell(Cell currentCell) {
    this.currentCell = this.currentCell != currentCell ? currentCell : null;
    ((DistalDendriteSegmentsModel)distalDendriteSegmentsTable.getModel()).setCell(this.currentCell);
    this.repaint();
  }

  private JTable initDistalDendriteSegmentsTable() {
    JTable table = new JTable(new DistalDendriteSegmentsModel());
    table.setPreferredScrollableViewportSize(new Dimension(100, 20));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    return table;
  }

  private JTable initSegmentDistalSynapsesTable() {
    JTable table = new JTable(new SegmentDistalSynapsesModel());
    table.setPreferredScrollableViewportSize(new Dimension(100, 20));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.getColumnModel().getColumn(0).setCellRenderer(new UIUtils.PermanenceRenderer());
    table.getColumnModel().getColumn(3).setCellRenderer(new UIUtils.PositionRenderer());
    return table;
  }

  public JTable getSegmentDistalSynapsesTable() {
    return segmentDistalSynapsesTable;
  }

  private class CellAttributesInfo extends UIUtils.TextColumnInfo {

    @Override
    protected Map<String, String> getAttributeMap() {
      Cell cell = currentCell;
      Map<String, String> result = new LinkedHashMap<String, String>();
      if (cell != null) {
        result.put("Index", cell.getCellIndex() + "");
        result.put("Position",
                   "X:" + (cell.getBelongsToColumn().getPosition().x) + ", Y:" + cell.getBelongsToColumn().getPosition().y);
        result.put("Active", cell.getActiveState(Cell.NOW) ? "Yes" : "No");
        result.put("Predictive", cell.getPredictiveState(Cell.NOW) ? "Yes" : "No");
        result.put("Learn", cell.getLearnState(Cell.NOW) ? "Yes" : "No");
        result.put("Seg. Num", cell.getSegments().size() + "");
        result.put("Seg. Updates Num", cell.getSegmentUpdates().size() + "");
      }
      return result;
    }
  }

  class DistalDendriteSegmentsModel extends AbstractTableModel {
    private java.util.List<DistalDendriteSegment> segments = null;
    private String[] columnNames = {
            "Sequence",
            "Active",
            "Learn",
            "Synapses N"
    };

    public void setCell(Cell cell) {
      segments = cell != null ? cell.getSegments() : null;
      this.fireTableDataChanged();
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

    @Override public Object getValueAt(int rowIndex, int columnIndex) {
      Object value = null;
      if (segments != null) {
        DistalDendriteSegment row = segments.get(rowIndex);
        switch (columnIndex) {
          case 0:
            value = row.isSequenceSegment();
            break;
          case 1:
            value = row.segmentActive(Cell.NOW, Cell.State.ACTIVE);
            break;
          case 2:
            value = row.segmentActive(Cell.NOW, Cell.State.LEARN);
            break;
          case 3:
            value = row.size();
            break;
          default:
            value = null;
        }
      }
      return value;
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
      Class result;
      switch (columnIndex) {
        case 0:
          result = Boolean.class;
          break;
        case 1:
          result = Boolean.class;
          break;
        case 2:
          result = Boolean.class;
          break;
        case 3:
          result = Integer.class;
          break;
        default:
          result = super.getColumnClass(
                  columnIndex);
      }
      return result;
    }
  }

  class SegmentDistalSynapsesModel extends AbstractTableModel {
    private java.util.List<Synapse.DistalSynapse> synapses = null;
    private String[] columnNames = {
            "Perm",
            "Act",
            "Inx",
            "Position"};

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
        switch (columnIndex) {
          case 0:
            value = row.getPermanence();
            break;
          case 1:
            value = row.getFromCell().getActiveState(Cell.NOW);
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
      return value;
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
      Class result;
      switch (columnIndex) {
        case 0:
          result = Double.class;
          break;
        case 1:
          result = Boolean.class;
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


}
