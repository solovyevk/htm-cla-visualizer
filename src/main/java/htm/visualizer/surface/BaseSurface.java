package htm.visualizer.surface;

import htm.utils.MathUtils;

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
  protected final Dimension dimension;
  /**
   * need two following parameters to buffer input on mouse drag event
   */
  private int lastVisitedInputIndex = -1;
  private boolean mouseDragged = false;

  protected BaseSurface(int xSize, int ySize) {
    this.dimension = new Dimension(xSize, ySize);
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
    double result = MathUtils.findMin((size.getHeight() - 2) / dimension.height,
                                      (size.getWidth() - 2) / dimension.width);
    return result < 1 ? 1 : (int)result - 1;
  }

  protected Point getElementStartPoint(int elementSpaceAllocation) {
    Dimension size = this.getSize();
    int startX = (int)(size.getWidth() - elementSpaceAllocation * this.dimension.width) / 2;
    int startY = (int)(size.getHeight() - elementSpaceAllocation * this.dimension.height) / 2;
    return new Point(startX, startY);
  }

  protected void doDrawing(Graphics2D g2d) {
    int elementSpaceAllocation = getElementSpaceAllocation();
    Point startPoint = getElementStartPoint(elementSpaceAllocation);

    int index = 0;
    for (int y = 0; y < dimension.height; y++) {
      for (int x = 0; x < dimension.width; x++) {
        drawElement(g2d, index, elementSpaceAllocation * x + startPoint.x, elementSpaceAllocation * y + startPoint.y,
                    elementSpaceAllocation - SPACE_BETWEEN_ELEMENTS, elementSpaceAllocation - SPACE_BETWEEN_ELEMENTS);
        index++;
      }
    }
  }

  private void mouseOver(int mouseX, int mouseY) {
    int elementSpaceAllocation = getElementSpaceAllocation(), elementWidth = (elementSpaceAllocation - BaseSurface.SPACE_BETWEEN_ELEMENTS);
    Point startPoint = getElementStartPoint(elementSpaceAllocation);
    int index = 0;
    outer:
    for (int y = 0; y < dimension.height; y++) {
      for (int x = 0; x < dimension.width; x++) {
        int elementCenterX = elementSpaceAllocation * x + startPoint.x + elementWidth / 2;
        int elementCenterY = elementSpaceAllocation * y + startPoint.y + elementWidth / 2;
        if (lastVisitedInputIndex != index && isMouseOverElement(mouseX, mouseY, elementCenterX, elementCenterY,
                                                                 elementWidth)) {
          fireElementMouseEnterEvent(index);
          lastVisitedInputIndex = index;
          break outer;
        }
        index++;
      }
    }
  }

  public Point getElementPositionByIndex(int byIndex) {
    int index = 0;
    for (int y = 0; y < dimension.height; y++) {
      for (int x = 0; x < dimension.width; x++) {
        if (byIndex == index) {
          return new Point(x, y);
        }
        index++;
      }
    }
    throw new IllegalArgumentException("No Element found by Index:" + byIndex);
  }

  public Rectangle getElementAreaByIndex(int byIndex) {
    return getElementArea(getElementPositionByIndex(byIndex));
  }

  public Rectangle getElementArea(Point position) {
    int elementSpaceAllocation = getElementSpaceAllocation();
    Point startPoint = getElementStartPoint(elementSpaceAllocation);
    return new Rectangle(elementSpaceAllocation * position.x + startPoint.x,
                         elementSpaceAllocation * position.y + startPoint.y,
                         elementSpaceAllocation - BaseSurface.SPACE_BETWEEN_ELEMENTS + 1,
                         elementSpaceAllocation - BaseSurface.SPACE_BETWEEN_ELEMENTS + 1);


  }


  /**
   * Return element area scaled by scaleFactor parameter
   *
   * @param position
   * @param scaleFactor
   * @return
   */

  public Rectangle getElementAreaWithScale(Point position, double scaleFactor) {
    Rectangle outsideArea = getElementArea(position);
    double newWidth = outsideArea.getWidth() * scaleFactor, newHeight = outsideArea.getHeight() * scaleFactor,
            newX = outsideArea.getX() - (newWidth - outsideArea.getWidth()) / 2, newY = outsideArea.getY() - (newHeight - outsideArea.getHeight()) / 2;
    return new Rectangle((int)newX, (int)newY, (int)newWidth, (int)newHeight);
  }

  public Rectangle getElementAreaWithScale(int byIndex, double scaleFactor) {
    return getElementAreaWithScale(getElementPositionByIndex(byIndex), scaleFactor);
  }


  protected abstract boolean isMouseOverElement(int mouseX, int mouseY, int elementCenterX, int elementCenterY,
                                                int elementWidth);

  protected abstract void drawElement(Graphics2D g2d, int index, int x, int y, int width, int height);


  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D)g;
    doDrawing(g2d);
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

  /**
   * the amount of elements over x
   */
  public int getXSize() {
    return dimension.width;
  }

  /**
   * the amount of elements over y
   */
  public int getYSize() {
    return dimension.height;
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

  public static class CircleElementsSurface extends BaseSurface {
    protected CircleElementsSurface(int xSize, int ySize) {
      super(xSize, ySize);
    }

    /*
    Just draw empty circle.
     */
    @Override protected void drawElement(Graphics2D g2d, int index, int x, int y, int width, int height) {
      g2d.setColor(activeColor);
      g2d.drawOval(x, y, width, height);
    }

    @Override
    protected boolean isMouseOverElement(int mouseX, int mouseY, int elementCenterX, int elementCenterY,
                                         int elementWidth) {
            /*circle eq in coordinates*/
      return Math.pow(mouseX - elementCenterX, 2) + Math.pow(mouseY - elementCenterY, 2) - Math.pow(elementWidth / 2,
                                                                                                    2) <= 0;
    }
  }

  public static class SquareElementsSurface extends BaseSurface {
    protected SquareElementsSurface(int xSize, int ySize) {
      super(xSize, ySize);
    }

    /*Just draw empty square*/
    @Override protected void drawElement(Graphics2D g2d, int index, int x, int y, int width, int height) {
      g2d.setColor(activeColor);
      g2d.drawRect(x, y, width, height);
    }

    @Override
    protected boolean isMouseOverElement(int mouseX, int mouseY, int elementCenterX, int elementCenterY,
                                         int elementWidth) {
      return MathUtils.inRange(mouseX, elementCenterX - elementWidth / 2, elementCenterX + elementWidth / 2) &&
             MathUtils.inRange(mouseY, elementCenterY - elementWidth / 2, elementCenterY + elementWidth / 2);
    }
  }
}
