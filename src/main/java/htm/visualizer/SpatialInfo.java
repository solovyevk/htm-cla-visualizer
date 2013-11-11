package htm.visualizer;

import htm.model.Column;
import htm.model.Synapse;
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

  private static final Color TEXT_COLOR = Color.black;


  public SpatialInfo() {
    this.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.NORTH;
    c.weighty = 1.0;
    c.weightx = 1.5;
    ColumnAttributesInfo left = new ColumnAttributesInfo();
    left.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                    "Column Properties",
                                                    TitledBorder.CENTER,
                                                    TitledBorder.TOP));
    this.add(left, c);
    c.gridx = 1;
    c.weightx = 2;
    //Create the scroll pane and add the table to it.
    proximalSynapsesTable = initSynapsesTable();
    JScrollPane right = new JScrollPane(proximalSynapsesTable);
    right.setBackground(Color.WHITE);
    //Add the scroll pane to this panel.
    add(right);
    right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                     "Proximal Synapses",
                                                     TitledBorder.CENTER,
                                                     TitledBorder.TOP));


    this.add(right, c);
  }

  public JTable getSynapsesTable(){
    return proximalSynapsesTable;
  }

  private JTable initSynapsesTable() {
    JTable table = new JTable(new ProximalSynapsesModel());
    table.setPreferredScrollableViewportSize(new Dimension(100, 70));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.getColumnModel().getColumn(0).setCellRenderer(new PermanenceRenderer());
    table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
      {
        this.setHorizontalAlignment(SwingConstants.RIGHT);
      }

      @Override protected void setValue(Object value) {
        setText((value == null) ? "" : DF_2.format(value));
      }
    });
    table.getColumnModel().getColumn(2).setPreferredWidth(30);
    table.getColumnModel().getColumn(3).setPreferredWidth(30);
    table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
      {
        this.setHorizontalAlignment(SwingConstants.CENTER);
      }

      @Override protected void setValue(Object value) {
        Point point = (Point)value;
        super.setValue("X:" + (point.x) + ", Y:" + point.y);
      }
    });
    return table;
  }


  public void setCurrentColumn(Column currentColumn) {
    this.currentColumn = this.currentColumn != currentColumn ? currentColumn : null;
    ((ProximalSynapsesModel)proximalSynapsesTable.getModel()).setColumn(this.currentColumn);
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
      drawPropertyParagraph(graphics2D, columnAttributes, 140, 10, 30);
    }


    private Map<String, String> getColumnAttributeMap(Column column) {
      Map<String, String> result = new LinkedHashMap<String, String>();

      if (column != null) {
        double overlapDutyCycle = column.getOverlapDutyCycle();
          double activeDutyCycle = column.getActiveDutyCycle();
        result.put("Index", column.getIndex() + "");
        result.put("Position", "X:" + (column.getPosition().x) + ", Y:" + column.getPosition().y);
        result.put("Active", column.isActive() ? "Yes" : "No");
        result.put("Active Duty Cycle", DF_4.format(column.getActiveDutyCycle()));
        result.put("Boost", DF_2.format(column.getBoost()));
        result.put("Overlap", DF_2.format(column.getOverlap()));
        result.put("Overlap Duty Cycle", DF_4.format(column.getOverlapDutyCycle()));
        result.put("Neighbors Count", column.getNeighbors(
                column.getRegion().getAverageReceptiveFieldSize()).size() + "");
        result.put("Connected Synapses", column.getConnectedSynapses().size() + "");
        result.put("Active Synapses", column.getActiveConnectedSynapses().size() + "");
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
        case 1:
          result = Double.class;
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
      Rectangle insideRec = new Rectangle(10, 2, size.height - 4, size.height - 4);
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

