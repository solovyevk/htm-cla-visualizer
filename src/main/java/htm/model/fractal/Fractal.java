package htm.model.fractal;

import java.util.List;

public interface Fractal<P, E> {
  public void reset();
  public P getOwner();
  public void addElement(E element);
  public void removeElement(E element);
  public E getElementByIndex(int inx);
  public List<E> getElements();
}


