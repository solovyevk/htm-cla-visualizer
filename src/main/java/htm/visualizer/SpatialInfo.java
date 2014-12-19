package htm.visualizer;

import htm.model.Column;
import htm.model.Synapse;
import htm.model.space.BaseSpace;
import htm.utils.UIUtils;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;


public class SpatialInfo extends JPanel {
  private Column currentColumn;
  private final JTable proximalSynapsesTable;
  private final JTable neighborColumnsTable;


  public SpatialInfo() {
    this.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.NORTH;
    c.weighty = 1.0;
    c.weightx = 1.4;
    ColumnAttributesInfo left = new ColumnAttributesInfo();
    left.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                    "Column Properties",
                                                    TitledBorder.CENTER,
                                                    TitledBorder.TOP));
    this.add(left, c);
    c.gridx = 1;
    c.weightx = 1.8;
    //Create the scroll pane and add the table to it.
    proximalSynapsesTable = initSynapsesTable();
    JComponent center = (new JPanel() {
      private JPanel init() {
        this.setBackground(Color.WHITE);
        this.setLayout(new BorderLayout());
        add(new JScrollPane(proximalSynapsesTable), BorderLayout.CENTER);
        proximalSynapsesTable.setFillsViewportHeight(true);
        return this;
      }
    }.init());
    add(center);
    center.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                      "Proximal Synapses",
                                                      TitledBorder.CENTER,
                                                      TitledBorder.TOP));


    this.add(center, c);
    c.gridx = 2;
    c.weightx = 2.2;
    neighborColumnsTable = initNeighborColumnsTable();
    JComponent right = (new JPanel() {
      private JPanel init() {
        this.setBackground(Color.WHITE);
        this.setLayout(new BorderLayout());
        add(new JScrollPane(neighborColumnsTable), BorderLayout.CENTER);
        neighborColumnsTable.setFillsViewportHeight(true);
        return this;
      }
    }.init());
    add(right);
    right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                     "Neighbor Columns",
                                                     TitledBorder.CENTER,
                                                     TitledBorder.TOP));
    this.add(right, c);


  }

  private JTable initSynapsesTable() {
    JTable table = new JTable(new ProximalSynapsesModel());
    table.setAutoCreateRowSorter(true);
    table.getColumnModel().getColumn(0).setCellRenderer(new UIUtils.PermanenceRenderer());
    table.getColumnModel().getColumn(1).setCellRenderer(new UIUtils.SmallDoubleRenderer());
    table.getColumnModel().getColumn(2).setPreferredWidth(50);
    table.getColumnModel().getColumn(3).setPreferredWidth(50);
    table.getColumnModel().getColumn(4).setCellRenderer(new UIUtils.PositionRenderer());
    return table;
  }

  public JTable getSynapsesTable() {
    return proximalSynapsesTable;
  }

  public JTable getNeighborColumnsTable() {
    return neighborColumnsTable;
  }


  private JTable initNeighborColumnsTable() {
    JTable table = new JTable(new NeighborColumnsModel());
    table.setAutoCreateRowSorter(true);
    table.getColumnModel().getColumn(0).setCellRenderer(new UIUtils.SmallDoubleRenderer());
    table.getColumnModel().getColumn(1).setCellRenderer(new UIUtils.SmallDoubleRenderer());
    table.getColumnModel().getColumn(1).setPreferredWidth(50);
    table.getColumnModel().getColumn(2).setCellRenderer(new UIUtils.SmallDoubleRenderer());
    table.getColumnModel().getColumn(3).setCellRenderer(new UIUtils.SmallDoubleRenderer());
    table.getColumnModel().getColumn(4).setCellRenderer(new UIUtils.SmallDoubleRenderer());
    table.getColumnModel().getColumn(4).setPreferredWidth(50);
    table.getColumnModel().getColumn(5).setPreferredWidth(40);
    table.getColumnModel().getColumn(6).setPreferredWidth(40);
    table.getColumnModel().getColumn(7).setCellRenderer(new UIUtils.PositionRenderer());
    return table;
  }


  public void setCurrentColumn(Column currentColumn) {
    this.currentColumn = this.currentColumn != currentColumn ? currentColumn : null;
    ((ProximalSynapsesModel)proximalSynapsesTable.getModel()).setColumn(this.currentColumn);
    ((NeighborColumnsModel)neighborColumnsTable.getModel()).setColumn(this.currentColumn);
    this.repaint();
  }


  private class ColumnAttributesInfo extends UIUtils.TextColumnInfo {

    @Override
    protected Map<String, String> getAttributeMap() {
      Column column = currentColumn;
      Map<String, String> result = new LinkedHashMap<String, String>();
      if (column != null) {
        double inhibitionRadius = column.getOwner().getAverageReceptiveFieldSize();
        result.put("Index", column.getIndex() + "");
        result.put("Position", "X:" + (column.getPosition().x) + ", Y:" + column.getPosition().y);
        result.put("Active", column.isActive() ? "Yes" : "No");
        result.put("Active Duty Cycle", UIUtils.DF_4.format(column.getActiveDutyCycle()));
        result.put("Max Duty Cycle", UIUtils.DF_4.format(column.getMaxDutyCycle(inhibitionRadius)));
        result.put("Boost", UIUtils.DF_2.format(column.getBoost()));
        result.put("Overlap", UIUtils.DF_2.format(column.getOverlap()));
        result.put("Over. Duty Cycle", UIUtils.DF_4.format(column.getOverlapDutyCycle()));
        result.put("Neighbors Count", column.getNeighbors(inhibitionRadius).size() + "");
        result.put("Connected Syn.", column.getConnectedSynapses().size() + "");
        result.put("Active Syn.", column.getActiveConnectedSynapses().size() + "");
        result.put("Avg. Rec. Field", UIUtils.DF_2.format(inhibitionRadius) + "");
      }
      return result;
    }

  }

  class NeighborColumnsModel extends AbstractTableModel {
    private Column column = null;
    private java.util.List<Column> neighbors = null;
    private final String[] columnNames = {
            "Overlap",
            "Dist",
            "ADC",
            "ODC",
            "Boost",
            "Act",
            "Inx",
            "Position"};

    public void setColumn(Column column) {
      this.column = column != null ? column : null;
      neighbors = column != null ? column.getNeighbors(column.getOwner().getAverageReceptiveFieldSize()) : null;
      this.fireTableDataChanged();
    }

    @Override public int getRowCount() {
      return neighbors == null ? 0 : neighbors.size();
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
      if (neighbors != null && column != null) {
        Column row = neighbors.get(rowIndex);
        switch (columnIndex) {
          case 0:
            value = row.getOverlap();
            break;
          case 1:
            value = BaseSpace.getDistance(column.getPosition(), row.getPosition());
            break;
          case 2:
            value = row.getActiveDutyCycle();
            break;
          case 3:
            value = row.getOverlapDutyCycle();
            break;
          case 4:
            value = row.getBoost();
            break;
          case 5:
            value = row.isActive();
            break;
          case 6:
            value = row.getIndex();
            break;
          case 7:
            value = new UIUtils.SortablePoint(row.getPosition());
            break;
          default:
            value = null;
        }
      }
      return value;
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
      Class<?> result;
      switch (columnIndex) {
        case 0:
          result = Double.class;
          break;
        case 1:
          result = Double.class;
          break;
        case 2:
          result = Double.class;
          break;
        case 3:
          result = Double.class;
          break;
        case 4:
          result = Double.class;
          break;
        case 5:
          result = Boolean.class;
          break;
        case 6:
          result = Integer.class;
          break;
        case 7:
          result = UIUtils.SortablePoint.class;
          break;
        default:
          result = super.getColumnClass(
                  columnIndex);
      }
      return result;
    }

  }

  class ProximalSynapsesModel extends AbstractTableModel {
    private java.util.List<Synapse.ProximalSynapse> synapses = null;
    private final String[] columnNames = {
            "Perm",
            "Dist",
            "I-Act",
            "I-Inx",
            "I-Position"};

    public void setColumn(Column column) {
      synapses = column != null ? column.getPotentialSynapses() : null;
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
        Synapse.ProximalSynapse row = synapses.get(rowIndex);
        switch (columnIndex) {
          case 0:
            value = row.getPermanence();
            break;
          case 1:
            value = row.getDistanceToColumn();
            break;
          case 2:
            value = row.getConnectedSensoryInput().getValue();
            break;
          case 3:
            value = row.getConnectedSensoryInput().getIndex();
            break;
          case 4:
            value = new UIUtils.SortablePoint(row.getConnectedSensoryInput().getPosition());
            break;
          default:
            value = null;
        }
      }
      return value;
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
      Class<?> result;
      switch (columnIndex) {
        case 0:
          result = Double.class;
          break;
        case 1:
          result = Double.class;
          break;
        case 2:
          result = Boolean.class;
          break;
        case 3:
          result = Integer.class;
          break;
        case 4:
          result = UIUtils.SortablePoint.class;
          break;
        default:
          result = super.getColumnClass(
                  columnIndex);
      }
      return result;
    }
  }


}

