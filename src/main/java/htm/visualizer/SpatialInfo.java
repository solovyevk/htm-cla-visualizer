package htm.visualizer;

import htm.model.Column;
import htm.model.Synapse;
import htm.model.space.BaseSpace;
import htm.visualizer.surface.SensoryInputSurface;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public class SpatialInfo extends JPanel {
  DecimalFormat DF_4 = new DecimalFormat("##0.0000");
  DecimalFormat DF_2 = new DecimalFormat("##0.00");
  private Column currentColumn;
  private JTable proximalSynapsesTable;
  private JTable neighborColumnsTable;


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
    c.weightx = 2.0;
    //Create the scroll pane and add the table to it.
    proximalSynapsesTable = initSynapsesTable();
    JScrollPane center = new JScrollPane(proximalSynapsesTable);
    center.setBackground(Color.WHITE);
    //Add the scroll pane to this panel.
    add(center);
    center.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                      "Proximal Synapses",
                                                      TitledBorder.CENTER,
                                                      TitledBorder.TOP));


    this.add(center, c);
    c.gridx = 2;
    c.weightx = 2.0;
    neighborColumnsTable = initNeighborColumnsTable();
    JScrollPane right = new JScrollPane(neighborColumnsTable);
    right.setBackground(Color.WHITE);
    add(right);
    right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                     "Neighbor Columns",
                                                     TitledBorder.CENTER,
                                                     TitledBorder.TOP));
    this.add(right, c);


  }

  private JTable initSynapsesTable() {
    JTable table = new JTable(new ProximalSynapsesModel());
    table.setPreferredScrollableViewportSize(new Dimension(100, 70));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.getColumnModel().getColumn(0).setCellRenderer(new PermanenceRenderer());
    table.getColumnModel().getColumn(1).setCellRenderer(new SmallDoubleRenderer());
    //table.getColumnModel().getColumn(2).setPreferredWidth(30);
    //table.getColumnModel().getColumn(3).setPreferredWidth(30);
    table.getColumnModel().getColumn(4).setCellRenderer(new PositionRenderer());
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
    table.setPreferredScrollableViewportSize(new Dimension(100, 70));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.getColumnModel().getColumn(0).setCellRenderer(new SmallDoubleRenderer());
    table.getColumnModel().getColumn(1).setCellRenderer(new SmallDoubleRenderer());
    //table.getColumnModel().getColumn(2).setPreferredWidth(30);
    //table.getColumnModel().getColumn(3).setPreferredWidth(30);
    table.getColumnModel().getColumn(4).setCellRenderer(new PositionRenderer());
    return table;
  }


  public void setCurrentColumn(Column currentColumn) {
    this.currentColumn = this.currentColumn != currentColumn ? currentColumn : null;
    ((ProximalSynapsesModel)proximalSynapsesTable.getModel()).setColumn(this.currentColumn);
    ((NeighborColumnsModel)neighborColumnsTable.getModel()).setColumn(this.currentColumn);
    this.repaint();
  }

  private class ColumnAttributesInfo extends JPanel {

    private ColumnAttributesInfo() {
      setBackground(Color.WHITE);
    }

    @Override public void paint(Graphics g) {
      super.paint(g);
      Graphics2D graphics2D = (Graphics2D)g;
      Map<String, String> columnAttributes = getColumnAttributeMap(currentColumn);
      drawPropertyParagraph(graphics2D, columnAttributes, 100, 5, 30);
    }


    private Map<String, String> getColumnAttributeMap(Column column) {
      Map<String, String> result = new LinkedHashMap<String, String>();
      if (column != null) {
        double inhibitionRadius = column.getRegion().getAverageReceptiveFieldSize();
        result.put("Index", column.getIndex() + "");
        result.put("Position", "X:" + (column.getPosition().x) + ", Y:" + column.getPosition().y);
        result.put("Active", column.isActive() ? "Yes" : "No");
        result.put("Active Duty Cycle", DF_4.format(column.getActiveDutyCycle()));
        result.put("Max Duty Cycle", DF_4.format(column.getMaxDutyCycle(inhibitionRadius)));
        result.put("Boost", DF_2.format(column.getBoost()));
        result.put("Overlap", DF_2.format(column.getOverlap()));
        result.put("Over. Duty Cycle", DF_4.format(column.getOverlapDutyCycle()));
        result.put("Neighbors Count", column.getNeighbors(inhibitionRadius).size() + "");
        result.put("Connected Syn.", column.getConnectedSynapses().size() + "");
        result.put("Active Syn.", column.getActiveConnectedSynapses().size() + "");
        result.put("Avg. Rec. Field", DF_2.format(inhibitionRadius) + "");
      }
      return result;
    }

    protected float drawPropertyParagraph(Graphics2D g2, Map<String, String> properties, float width, float x,
                                          float y) {
      FontRenderContext frc = g2.getFontRenderContext();
      float drawPosY = y;
      Font nameFont = new Font("Helvetica", Font.BOLD, 12);
      Font valueFont = new Font("Helvetica", Font.PLAIN, 12);
      Set<String> names = properties.keySet();
      for (String name : names) {
        String value = properties.get(name);
        TextLayout nameLayout = new TextLayout(name + ":", nameFont, frc);
        TextLayout valueLayout = new TextLayout(value, valueFont, frc);
        // Set position to the index of the first character in the paragraph.
        float drawPosX;
        drawPosX = (float)x + width - nameLayout.getAdvance();
        // Move y-coordinate by the ascent of the layout.
        drawPosY += nameLayout.getAscent();
        // Draw the TextLayout at (drawPosX, drawPosY).
        nameLayout.draw(g2, drawPosX, drawPosY);
        double newX = (float)x + width + 4;
        valueLayout.draw(g2, (float)newX, drawPosY);
        // Move y-coordinate in preparation for next layout.
        drawPosY += nameLayout.getDescent() + nameLayout.getLeading();
      }
      return drawPosY;
    }

  }

  class NeighborColumnsModel extends AbstractTableModel {
    private Column column = null;
    private java.util.List<Column> neighbors = null;
    private String[] columnNames = {
            "Overlap",
            "Distance",
            "Active",
            "Index",
            "Position"};

    public void setColumn(Column column) {
      this.column = column != null ? column : null;
      neighbors = column != null ? column.getNeighbors(column.getRegion().getAverageReceptiveFieldSize()) : null;
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
            value = row.isActive();
            break;
          case 3:
            value = row.getIndex();
            break;
          case 4:
            value = new SortablePoint(row.getPosition());
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
          result = Double.class;
          break;
        case 2:
          result = Boolean.class;
          break;
        case 3:
          result = Integer.class;
          break;
        case 4:
          result = SortablePoint.class;
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
    private String[] columnNames = {
            "Permanence",
            "Distance",
            "I-Active",
            "I-Index",
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
            value = new SortablePoint(row.getConnectedSensoryInput().getPosition());
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
          result = Double.class;
          break;
        case 2:
          result = Boolean.class;
          break;
        case 3:
          result = Integer.class;
          break;
        case 4:
          result = SortablePoint.class;
          break;
        default:
          result = super.getColumnClass(
                  columnIndex);
      }
      return result;
    }
  }


  static class SortablePoint extends Point implements Comparable<SortablePoint> {
    SortablePoint(Point p) {
      super(p);
    }

    @Override public int compareTo(SortablePoint point) {
      return this.getSquare() - point.getSquare();
    }

    int getSquare() {
      return (int)(Math.pow(x, 2) + Math.pow(y, 2));
    }
  }

  class SmallDoubleRenderer extends DefaultTableCellRenderer {
    {
      this.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override protected void setValue(Object value) {
      super.setValue(DF_2.format(value));
    }
  }

  class PositionRenderer extends DefaultTableCellRenderer {
    {
      this.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override protected void setValue(Object value) {
      Point point = (Point)value;
      super.setValue("X:" + (point.x) + ", Y:" + point.y);
    }
  }


  class PermanenceRenderer extends DefaultTableCellRenderer
          implements TableCellRenderer {
    private double permanence = 0;

    { // initializer block
      this.setHorizontalAlignment(SwingConstants.RIGHT);
    }
   /* public PermanenceRenderer() {
      this.setHorizontalAlignment(SwingConstants.CENTER);
    }*/

    @Override public void paint(Graphics g) {
      super.paint(g);
      Dimension size = this.getSize();
      Rectangle insideRec = new Rectangle(2, 2, size.height - 4, size.height - 4);
      Graphics2D g2d = (Graphics2D)g;
      SensoryInputSurface.renderSynapse(g2d, permanence, insideRec);
      g2d.setColor(Color.BLACK);
      //g2d.drawString(DF_4.format(permanence) + "", size.height + 4, 12);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
      this.permanence = (Double)value;
      return super.getTableCellRendererComponent(table, DF_4.format(value), isSelected, hasFocus, row,
                                                 column);
    }
  }

}

