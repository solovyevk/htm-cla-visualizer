/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.space;


import htm.utils.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.List;


public abstract class BaseSpace<P, E extends Element<?, ?>> extends htm.model.fractal.Composite<P, E> {
  private static final Log LOG = LogFactory.getLog(
          BaseSpace.class);

  private final Dimension dimension;


  protected BaseSpace(int xSize, int ySize) {
    this.dimension = new Dimension(xSize, ySize);
  }
  /*Have to call it after construction and param initialization*/
  protected void initElementSpace() {
    int xSize = this.dimension.width; int ySize = this.dimension.height;
    int index = 0;
    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        elementList.add(index, createElement(index, new Point(x, y)));
        index++;
      }
    }
  }

  BaseSpace(Dimension dimension) {
    this(dimension.width, dimension.height);
  }

  public int getLongSide() {
    return Math.max(dimension.width, dimension.height);
  }

  public int getShortSide() {
    return Math.min(dimension.width, dimension.height);
  }

  protected abstract E createElement(int index, Point position);

  public E getElementByPosition(Point position) {
    for (E element : elementList) {
      if (element.getPosition().x == position.x && element.getPosition().y == position.y) {
        return element;
      }
    }
    throw new IllegalArgumentException("There in no element by this position" + position);
  }


  public List<E> getAllWithinRadius(final Point center, final double radius) {
    CollectionUtils.Predicate<E> withinRadius = new CollectionUtils.Predicate<E>() {
      @Override public boolean apply(E element) {
        return Math.pow(center.x - element.getPosition().x, 2) + Math.pow(center.y - element.getPosition().y,
                                                                          2) <= Math.pow(radius, 2);
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


}

