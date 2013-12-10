/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.utils;

import htm.visualizer.surface.SensoryInputSurface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;

public enum UIUtils {
  INSTANCE;
  public static final DecimalFormat DF_4 = new DecimalFormat("##0.0000");
  public static final DecimalFormat DF_2 = new DecimalFormat("##0.00");

  private static final Log LOG = LogFactory.getLog(UIUtils.class);
  public final static Color LIGHT_BLUE = new Color(153, 204, 255);
  public static Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(0, 4, 0, 4);
  public static Border LIGHT_GRAY_BORDER = BorderFactory.createLineBorder(Color.lightGray);

  private UIUtils() {
  }

  public ImageIcon createImageIcon(String path) {
    java.net.URL imgURL = getClass().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL);
    } else {
      LOG.error("Couldn't find file: " + path);
      throw new IllegalArgumentException("Couldn't find file: " + path);
    }
  }

  /*
   * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
   *
   * Redistribution and use in source and binary forms, with or without
   * modification, are permitted provided that the following conditions
   * are met:
   *
   *   - Redistributions of source code must retain the above copyright
   *     notice, this list of conditions and the following disclaimer.
   *
   *   - Redistributions in binary form must reproduce the above copyright
   *     notice, this list of conditions and the following disclaimer in the
   *     documentation and/or other materials provided with the distribution.
   *
   *   - Neither the name of Oracle or the names of its
   *     contributors may be used to endorse or promote products derived
   *     from this software without specific prior written permission.
   *
   * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
   * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
   * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
   * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
   * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
   */

  /**
   * A 1.4 file that provides utility methods for
   * creating form- or grid-style layouts with SpringLayout.
   * These utilities are used by several programs, such as
   * SpringBox and SpringCompactGrid.
   */

  /**
   * Aligns the first <code>rows</code> * <code>cols</code>
   * components of <code>parent</code> in
   * a grid. Each component in a column is as wide as the maximum
   * preferred width of the components in that column;
   * height is similarly determined for each row.
   * The parent is made just big enough to fit them all.
   *
   * @param rows     number of rows
   * @param cols     number of columns
   * @param initialX x location to start the grid at
   * @param initialY y location to start the grid at
   * @param xPad     x padding between cells
   * @param yPad     y padding between cells
   */
  public static void makeSpringCompactGrid(Container parent,
                                           int rows, int cols,
                                           int initialX, int initialY,
                                           int xPad, int yPad) {
    SpringLayout layout;
    try {
      layout = (SpringLayout)parent.getLayout();
    } catch (ClassCastException exc) {
      System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
      return;
    }

    //Align all cells in each column and make them the same width.
    Spring x = Spring.constant(initialX);
    for (int c = 0; c < cols; c++) {
      Spring width = Spring.constant(0);
      for (int r = 0; r < rows; r++) {
        width = Spring.max(width,
                           getConstraintsForCell(r, c, parent, cols).
                                   getWidth());
      }
      for (int r = 0; r < rows; r++) {
        SpringLayout.Constraints constraints =
                getConstraintsForCell(r, c, parent, cols);
        constraints.setX(x);
        constraints.setWidth(width);
      }
      x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
    }

    //Align all cells in each row and make them the same height.
    Spring y = Spring.constant(initialY);
    for (int r = 0; r < rows; r++) {
      Spring height = Spring.constant(0);
      for (int c = 0; c < cols; c++) {
        height = Spring.max(height,
                            getConstraintsForCell(r, c, parent, cols).
                                    getHeight());
      }
      for (int c = 0; c < cols; c++) {
        SpringLayout.Constraints constraints =
                getConstraintsForCell(r, c, parent, cols);
        constraints.setY(y);
        constraints.setHeight(height);
      }
      y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
    }

    //Set the parent's size.
    SpringLayout.Constraints pCons = layout.getConstraints(parent);
    pCons.setConstraint(SpringLayout.SOUTH, y);
    pCons.setConstraint(SpringLayout.EAST, x);
  }

  /**
   * Aligns the first <code>rows</code> * <code>cols</code>
   * components of <code>parent</code> in
   * a grid. Each component is as big as the maximum
   * preferred width and height of the components.
   * The parent is made just big enough to fit them all.
   *
   * @param rows     number of rows
   * @param cols     number of columns
   * @param initialX x location to start the grid at
   * @param initialY y location to start the grid at
   * @param xPad     x padding between cells
   * @param yPad     y padding between cells
   */
  public static void makeSpringGrid(Container parent,
                                    int rows, int cols,
                                    int initialX, int initialY,
                                    int xPad, int yPad) {
    SpringLayout layout;
    try {
      layout = (SpringLayout)parent.getLayout();
    } catch (ClassCastException exc) {
      System.err.println("The first argument to makeGrid must use SpringLayout.");
      return;
    }

    Spring xPadSpring = Spring.constant(xPad);
    Spring yPadSpring = Spring.constant(yPad);
    Spring initialXSpring = Spring.constant(initialX);
    Spring initialYSpring = Spring.constant(initialY);
    int max = rows * cols;

    //Calculate Springs that are the max of the width/height so that all
    //cells have the same size.
    Spring maxWidthSpring = layout.getConstraints(parent.getComponent(0)).
            getWidth();
    Spring maxHeightSpring = layout.getConstraints(parent.getComponent(0)).
            getHeight();
    for (int i = 1; i < max; i++) {
      SpringLayout.Constraints cons = layout.getConstraints(
              parent.getComponent(i));

      maxWidthSpring = Spring.max(maxWidthSpring, cons.getWidth());
      maxHeightSpring = Spring.max(maxHeightSpring, cons.getHeight());
    }

    //Apply the new width/height Spring. This forces all the
    //components to have the same size.
    for (int i = 0; i < max; i++) {
      SpringLayout.Constraints cons = layout.getConstraints(
              parent.getComponent(i));

      cons.setWidth(maxWidthSpring);
      cons.setHeight(maxHeightSpring);
    }

    //Then adjust the x/y constraints of all the cells so that they
    //are aligned in a grid.
    SpringLayout.Constraints lastCons = null;
    SpringLayout.Constraints lastRowCons = null;
    for (int i = 0; i < max; i++) {
      SpringLayout.Constraints cons = layout.getConstraints(
              parent.getComponent(i));
      if (i % cols == 0) { //start of new row
        lastRowCons = lastCons;
        cons.setX(initialXSpring);
      } else { //x position depends on previous component
        cons.setX(Spring.sum(lastCons.getConstraint(SpringLayout.EAST),
                             xPadSpring));
      }

      if (i / cols == 0) { //first row
        cons.setY(initialYSpring);
      } else { //y position depends on previous row
        cons.setY(Spring.sum(lastRowCons.getConstraint(SpringLayout.SOUTH),
                             yPadSpring));
      }
      lastCons = cons;
    }

    //Set the parent's size.
    SpringLayout.Constraints pCons = layout.getConstraints(parent);
    pCons.setConstraint(SpringLayout.SOUTH,
                        Spring.sum(
                                Spring.constant(yPad),
                                lastCons.getConstraint(SpringLayout.SOUTH)));
    pCons.setConstraint(SpringLayout.EAST,
                        Spring.sum(
                                Spring.constant(xPad),
                                lastCons.getConstraint(SpringLayout.EAST)));
  }

  /* Used by makeCompactGrid. */
  private static SpringLayout.Constraints getConstraintsForCell(
          int row, int col,
          Container parent,
          int cols) {
    SpringLayout layout = (SpringLayout)parent.getLayout();
    Component c = parent.getComponent(row * cols + col);
    return layout.getConstraints(c);
  }

  public static abstract class TextColumnInfo extends JPanel {
    protected float finishParagraphY;

    public TextColumnInfo() {
      setBackground(Color.WHITE);
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      Graphics2D graphics2D = (Graphics2D)g;
      Map<String, String> cellAttributes = getAttributeMap();
      finishParagraphY = drawPropertyParagraph(graphics2D, cellAttributes, 100, 5, 20);
    }


    abstract protected Map<String, String> getAttributeMap();

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

  public static class SortablePoint extends Point implements Comparable<SortablePoint> {
    public SortablePoint(Point p) {
      super(p);
    }

    @Override public int compareTo(SortablePoint point) {
      return this.getSquare() - point.getSquare();
    }

    int getSquare() {
      return (int)(Math.pow(x, 2) + Math.pow(y, 2));
    }
  }

  public static class SmallDoubleRenderer extends DefaultTableCellRenderer {
    {
      this.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override protected void setValue(Object value) {
      super.setValue(DF_2.format(value));
    }
  }

  public static class PositionRenderer extends DefaultTableCellRenderer {
    {
      this.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override protected void setValue(Object value) {
      Point point = (Point)value;
      super.setValue("X:" + (point.x) + ", Y:" + point.y);
    }
  }

  public static class PermanenceRenderer extends DefaultTableCellRenderer
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
