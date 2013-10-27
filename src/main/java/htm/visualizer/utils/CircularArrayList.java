/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.utils;

import java.util.ArrayList;


public class CircularArrayList<E> extends ArrayList<E> {

  private int maxCapacity;

  /**
   * ArrayList has a capacity, if full,
   * remove the oldest element
   *
   * @param capacity
   */
  public CircularArrayList(int capacity) {
    this.maxCapacity = capacity;
  }

  /**
   *
   * if full, remove oldest element
   * @param element
   */

  @Override public boolean add(E element) {
    boolean result = super.add(element);
    removeExcess();
    return result;
  }

  /**
   * if full, remove oldest element
   *
   * @param index
   * @param element
   */
  @Override
  public void add(int index, E element) {
    super.add(index, element);
    removeExcess();
  }

  private void removeExcess() {
    if (this.size() > maxCapacity) {
      this.remove(maxCapacity);
    }
  }
}
