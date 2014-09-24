/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.space;


import htm.model.fractal.Composite;

import java.awt.*;

public class Element<P, E>  extends Composite<P, E> {
  protected final Point position;
  private final int index;

  public Element(P owner, Point position, int index) {
    this.owner = owner;
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

  @Override
  public String toString() {
    return "locationSeed:" + getLocationSeed() + ", X:" + getPosition().x + ", Y:" + getPosition().y + ", index:" + getIndex();
  }
}
