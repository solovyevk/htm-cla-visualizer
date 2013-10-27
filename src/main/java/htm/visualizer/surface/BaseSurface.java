package htm.visualizer.surface;

import htm.visualizer.utils.MathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public abstract class BaseSurface extends JPanel {
  public static final int SPACE_BETWEEN_ELEMENTS = 4;
  public static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;
  public static final Color DEFAULT_ACTIVE_COLOR = Color.BLACK;

  protected Color activeColor = DEFAULT_ACTIVE_COLOR;
  /**
   * the amount of elements over x
   */
  protected final int xSize;
  /**
   * the amount of elements over y
   */
  protected final int ySize;
  /*
  need two following parameters to buffer input on mouse drag event
  */
  private int lastVisitedInputIndex = -1;
  private boolean mouseDragged = false;

  protected BaseSurface(int xSize, int ySize) {
    this.xSize = xSize;
    this.ySize = ySize;
    setBackground(DEFAULT_BACKGROUND_COLOR);
    addMouseListener(new MouseAdapter() {

      @Override
      public void mouseReleased(MouseEvent e) {
        lastVisitedInputIndex = -1;
        if (!mouseDragged) {
          mouseOver(e.getX(), e.getY());
        }
        mouseDragged = false;
      }
    });

    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        mouseDragged = true;
        mouseOver(e.getX(), e.getY());
      }
    });
  }

  protected int getElementSpaceAllocation() {
    Dimension size = this.getSize();
    double result = MathUtils.findMin((size.getHeight() - 2) / ySize, (size.getWidth() - 2) / xSize);
    return result < 1 ? 1 : (int)result - 1;
  }

  protected Point getElementStartPoint(int elementSpaceAllocation) {
    Dimension size = this.getSize();
    int startX = (int)(size.getWidth() - elementSpaceAllocation * this.xSize) / 2;
    int startY = (int)(size.getHeight() - elementSpaceAllocation * this.ySize) / 2;
    return new Point(startX, startY);
  }

  protected void doDrawing(Graphics g) {
    int elementSpaceAllocation = getElementSpaceAllocation();
    Point startPoint = getElementStartPoint(elementSpaceAllocation);
    Graphics2D g2d = (Graphics2D)g;
    int index = -1;
    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        index++;
        drawElement(g2d, index, elementSpaceAllocation * x + startPoint.x, elementSpaceAllocation * y + startPoint.y,
                    elementSpaceAllocation - SPACE_BETWEEN_ELEMENTS, elementSpaceAllocation - SPACE_BETWEEN_ELEMENTS);
      }
    }
  }

  private void mouseOver(int mouseX, int mouseY) {
    int elementSpaceAllocation = getElementSpaceAllocation(), elementWidth = (elementSpaceAllocation - BaseSurface.SPACE_BETWEEN_ELEMENTS);
    Point startPoint = getElementStartPoint(elementSpaceAllocation);
    int index = -1;
    outer:
    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        index++;
        int elementCenterX = elementSpaceAllocation * x + startPoint.x + elementWidth / 2;
        int elementCenterY = elementSpaceAllocation * y + startPoint.y + elementWidth / 2;
        if (lastVisitedInputIndex != index && isMouseOverElement(mouseX, mouseY, elementCenterX, elementCenterY,
                                                                 elementWidth)) {
          fireElementMouseEnterEvent(index);
          lastVisitedInputIndex = index;
          break outer;
        }
      }
    }
  }

  protected Rectangle getElementAreaByIndex(int byIndex) {
    int elementSpaceAllocation = getElementSpaceAllocation(), elementWidth = (elementSpaceAllocation - BaseSurface.SPACE_BETWEEN_ELEMENTS);
    Point startPoint = getElementStartPoint(elementSpaceAllocation);
    int index = -1;
    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        index++;
        if (byIndex == index) {
          return new Rectangle(elementSpaceAllocation * x + startPoint.x, elementSpaceAllocation * y + startPoint.y,
                               elementSpaceAllocation - BaseSurface.SPACE_BETWEEN_ELEMENTS,
                               elementSpaceAllocation - BaseSurface.SPACE_BETWEEN_ELEMENTS);


        }
      }
    }
    throw new IllegalArgumentException("No Element found by Index:" + byIndex);
  }


  protected abstract boolean isMouseOverElement(int mouseX, int mouseY, int elementCenterX, int elementCenterY,
                                                int elementWidth);

  protected abstract void drawElement(Graphics2D g2d, int index, int x, int y, int width, int height);


  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    doDrawing(g);
  }

  /*Custom events implementation*/
  private Collection<ElementMouseEnterListener> _listeners = new ArrayList<ElementMouseEnterListener>();

  public synchronized void addElementMouseEnterListener(ElementMouseEnterListener listener) {
    _listeners.add(listener);
  }

  public synchronized void removeElementMouseEnterListener(ElementMouseEnterListener listener) {
    _listeners.remove(listener);
  }

  private synchronized void fireElementMouseEnterEvent(int index) {
    ElementMouseEnterEvent event = new ElementMouseEnterEvent(this, index);
    Iterator i = _listeners.iterator();
    while (i.hasNext()) {
      ((ElementMouseEnterListener)i.next()).onElementMouseEnter(event);
    }
  }

  public static class ElementMouseEnterEvent extends java.util.EventObject {
    private final int index;

    public ElementMouseEnterEvent(Object source, int index) {
      super(source);
      this.index = index;
    }

    public int getIndex() {
      return index;
    }

  }

  public interface ElementMouseEnterListener {
    public void onElementMouseEnter(ElementMouseEnterEvent e);
  }

  public abstract static class CircleElementsSurface extends BaseSurface {
    protected CircleElementsSurface(int xSize, int ySize) {
      super(xSize, ySize);
    }

    @Override
    protected boolean isMouseOverElement(int mouseX, int mouseY, int elementCenterX, int elementCenterY,
                                         int elementWidth) {
            /*circle eq in coordinates*/
      return Math.pow(mouseX - elementCenterX, 2) + Math.pow(mouseY - elementCenterY, 2) - Math.pow(elementWidth / 2,
                                                                                                    2) <= 0;
    }
  }

  public abstract static class SquareElementsSurface extends BaseSurface {
    protected SquareElementsSurface(int xSize, int ySize) {
      super(xSize, ySize);
    }

    @Override
    protected boolean isMouseOverElement(int mouseX, int mouseY, int elementCenterX, int elementCenterY,
                                         int elementWidth) {
      return MathUtils.inRange(mouseX, elementCenterX - elementWidth / 2, elementCenterX + elementWidth / 2) &&
             MathUtils.inRange(mouseY, elementCenterY - elementWidth / 2, elementCenterY + elementWidth / 2);
    }
  }
}
