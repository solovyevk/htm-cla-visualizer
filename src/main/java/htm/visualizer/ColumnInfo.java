package htm.visualizer;

import htm.model.Column;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public class ColumnInfo extends JPanel {
  DecimalFormat DF_4 = new DecimalFormat("##0.0000");
  DecimalFormat DF_2 = new DecimalFormat("##0.00");
  private Column currentColumn;


  private static final Color TEXT_COLOR = Color.black;


  public ColumnInfo() {
    setBackground(Color.WHITE);
  }

  @Override public void paint(Graphics g) {
    super.paint(g);
    Graphics2D graphics2D = (Graphics2D)g;
    Map<String, String> columnAttributes = getColumnAttributeMap(this.currentColumn);
    drawPropertyParagraph(graphics2D, columnAttributes , 140, 10, 10);
  }


  public void setCurrentColumn(Column currentColumn) {
    this.currentColumn = this.currentColumn != currentColumn ? currentColumn : null;
    this.repaint();
  }

  private Map<String, String> getColumnAttributeMap(Column column) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    if (column != null) {
      result.put("Index", column.getIndex()+"");
      result.put("Position", "X:" + (column.getPosition().x) + ", Y:" + column.getPosition().y);
      result.put("Active", column.isActive() ? "Yes" : "No");
      result.put("Active Duty Cycle", DF_4.format(column.getActiveDutyCycle()));
      result.put("Overlap",  DF_2.format(column.getOverlap()));
      result.put("Overlap Duty Cycle", DF_4.format(column.getOverlapDutyCycle()));
      result.put("Neighbors Count", column.getNeighbors(column.getRegion().getAverageReceptiveFieldSize()).size() + "");
      result.put("Connected Synapses", column.getConnectedSynapses().size() +"");
      result.put("Active Synapses", column.getActiveConnectedSynapses().size() +"");
    }
    return result;
  }


  public static enum Alignment {RIGHT, LEFT, CENTER}


  /**
   * Draw paragraph.
   *
   * @param g2        Drawing graphic.
   * @param text      String to draw.
   * @param width     Paragraph's desired width.
   * @param x         Start paragraph's X-Position.
   * @param y         Start paragraph's Y-Position.
   * @param alignment Paragraph's alignment.
   * @return Next line Y-position to write to.
   */
  protected float drawParagraph(Graphics2D g2, String text, float width, float x, float y, Alignment alignment) {
    AttributedString attrString = new AttributedString(text);
    attrString.addAttribute(TextAttribute.FONT, new Font("Helvetica", Font.PLAIN, 12));
    AttributedCharacterIterator paragraph = attrString.getIterator();
    int paragraphStart = paragraph.getBeginIndex();
    int paragraphEnd = paragraph.getEndIndex();
    FontRenderContext frc = g2.getFontRenderContext();
    LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, frc);

    // Set break width to width of Component.
    float breakWidth = width;
    float drawPosY = y;
    // Set position to the index of the first character in the paragraph.
    lineMeasurer.setPosition(paragraphStart);

    // Get lines until the entire paragraph has been displayed.
    while (lineMeasurer.getPosition() < paragraphEnd) {
      // Retrieve next layout. A cleverer program would also cache
      // these layouts until the component is re-sized.
      TextLayout layout = lineMeasurer.nextLayout(breakWidth);
      // Compute pen x position.
      float drawPosX;
      switch (alignment) {
        case RIGHT:
          drawPosX = (float)x + breakWidth - layout.getAdvance();
          break;
        case CENTER:
          drawPosX = (float)x + (breakWidth - layout.getAdvance()) / 2;
          break;
        default:
          drawPosX = (float)x;
      }
      // Move y-coordinate by the ascent of the layout.
      drawPosY += layout.getAscent();

      // Draw the TextLayout at (drawPosX, drawPosY).
      layout.draw(g2, drawPosX, drawPosY);

      // Move y-coordinate in preparation for next layout.
      drawPosY += layout.getDescent() + layout.getLeading();
    }
    return drawPosY;
  }

  protected float drawPropertyParagraph(Graphics2D g2, Map<String, String> properties, float width, float x, float y) {
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

