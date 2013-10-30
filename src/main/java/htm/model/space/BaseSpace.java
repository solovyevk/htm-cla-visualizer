/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.space;

import htm.visualizer.utils.CollectionUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseSpace<E extends BaseSpace.Element> {
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

  public List<E> getElements(){
     return Collections.unmodifiableList(elementList);
  }


  public List<E> getAllWithinRadius(final Point center, final double radius) {
    final double adjustedRadius = radius < 1 ? Math.sqrt(Math.pow(this.getDimension().height, 2) + Math.pow(this.getDimension().width, 2)) : radius;
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
  }

}

