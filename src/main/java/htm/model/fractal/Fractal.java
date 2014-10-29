package htm.model.fractal;

import java.util.List;

public interface Fractal<P, E> {
  public void reset();
  public P getOwner();
  public E getElementByIndex(int inx);
  public List<E> getElements();
  public boolean addElement(E element);
  public boolean addAll(List<E> all);
  public void removeElement(E element);
}


