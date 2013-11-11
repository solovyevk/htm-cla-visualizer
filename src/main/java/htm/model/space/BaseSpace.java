/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.space;


import htm.utils.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseSpace<E extends BaseSpace.Element> {
  private static final Log LOG = LogFactory.getLog(
          BaseSpace.class);

  protected final java.util.List<E> elementList;
  private final Dimension dimension;


  public BaseSpace(int xSize, int ySize) {
    this.dimension = new Dimension(xSize, ySize);
    elementList = new ArrayList<E>(xSize * ySize);
    int index = 0;
    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        elementList.add(index, createElement(this, index, new Point(x, y)));
        index++;
      }
    }
  }

  public BaseSpace(Dimension dimension) {
    this(dimension.width, dimension.height);
  }

  public int getLongSide() {
    return Math.max(dimension.width, dimension.height);
  }

  public int getShortSide() {
    return Math.min(dimension.width, dimension.height);
  }

  protected abstract E createElement(BaseSpace<E> space, int index, Point position);

  public E getElementByPosition(Point position) {
    for (E element : elementList) {
      if (element.getPosition().x == position.x && element.getPosition().y == position.y) {
        return element;
      }
    }
    throw new IllegalArgumentException("There in no element by this position" + position);
  }

  public E getElementByIndex(int index) {
    return elementList.get(index);
  }

  public List<E> getElements() {
    return Collections.unmodifiableList(elementList);
  }


  public List<E> getAllWithinRadius(final Point center, final double radius) {
    final double adjustedRadius = radius < 1 ? Math.sqrt(Math.pow(this.getDimension().height, 2) + Math.pow(
            this.getDimension().width, 2)) : radius;
    CollectionUtils.Predicate<E> withinRadius = new CollectionUtils.Predicate<E>() {
      @Override public boolean apply(E element) {
        return Math.pow(center.x - element.getPosition().x, 2) + Math.pow(center.y - element.getPosition().y,
                                                                          2) <= Math.pow(adjustedRadius, 2);
      }
    };
    return CollectionUtils.filter(elementList, withinRadius);
  }

  public Dimension getDimension() {
    return dimension;
  }

  protected Point convertPositionToOtherSpace(Point srcPosition, Dimension srcDimension, Dimension targetDimension) {
    double xScale = targetDimension.getWidth() / srcDimension.getWidth();
    double yScale = targetDimension.getHeight() / srcDimension.getHeight();
    //apply function scale
    double xAdj = 0;// xScale/2 - (srcPosition.getX()/srcDimension.getWidth()) * xScale;
    double targetX = Math.min(Math.ceil(srcPosition.getX() * xScale + (xScale > 1 ? xAdj : 0)),
                           targetDimension.width - 1);
    //LOG.debug("xAdj:" + xAdj + ", X:" + srcPosition.getX() * xScale + ", adj. targetX :" + targetX);
    double yAdj = 0;// yScale/2 - (srcPosition.getY()/srcDimension.getHeight()) * yScale;
    //double yAdj = yScale / 2 - srcPosition.getY() / ((yScale + 2) * yScale);
    double targetY = Math.min(Math.ceil(srcPosition.getY() * yScale + (yScale > 1 ? yAdj : 0)),
                           targetDimension.height - 1);
    Point result = new Point();
    result.setLocation(targetX, targetY);
    return result;
  }

  /*
  helpers
   */
  public static double getDistance(Point pointOne, Point pointTwo) {
    int dX = pointOne.x - pointTwo.x;
    int dY = pointOne.y - pointTwo.y;
    return Math.sqrt(Math.pow(dX, 2) + Math.pow(dY, 2));
  }


  public static class Element {
    protected final Point position;
    private final int index;

    public Element(Point position, int index) {
      this.position = position;
      this.index = index;
    }

    public Point getPosition() {
      return position;
    }

    public int getIndex() {
      return index;
    }

    /**
     * Kind unique identifier for position
     */
    public int getLocationSeed() {
      int length = position.y == 0 ? 1 : (int)(Math.log10(position.y) + 1);
      return (position.x + 1) * (int)Math.pow(10, length) + position.y;
    }

    @Override public String toString() {
      return "locationSeed:" + getLocationSeed() + ", X:" + getPosition().x + ", Y:" + getPosition().y + ", index:" + getIndex();
    }
  }

}

