/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.fractal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Composite<P, E> implements Fractal<P, E> {
  protected final List<E> elementList = new ArrayList<E>();
  protected P owner;

  @Override public void reset() {
    //DO NOTHING BY DEFAULT
  }

  @Override public List<E> getElements() {
    return Collections.unmodifiableList(elementList);
  }

  @Override
  public P getOwner() {
    return owner;
  }

  @Override public void addElement(E element) {
    elementList.add(element);
  }

  @Override public void removeElement(E element) {
    elementList.remove(element);
  }

  @Override public E getElementByIndex(int index) {
      return elementList.get(index);
    }


}
