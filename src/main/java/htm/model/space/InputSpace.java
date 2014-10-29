/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.space;


import java.awt.*;
import java.util.List;
import java.util.NoSuchElementException;

public class InputSpace extends BaseSpace<InputSpace, InputSpace.Input> {
  public InputSpace(int xSize, int ySize) {
    super(xSize, ySize);
    initElementSpace();
  }

  public InputSpace(Dimension dimension) {
    super(dimension);
    initElementSpace();
  }

  @Override
  protected Input createElement(int index,
                                Point position) {
    return new Input(this, position, index, false);
  }


  public void setInputValue(int index, boolean value) {
    this.getElementByIndex(index).setValue(value);
  }

  public boolean getInputValue(int index) {
    return this.getElementByIndex(index).getValue();
  }


  public static class Input extends Element<InputSpace, Input> {
    private boolean value;

    public Input(InputSpace space, Point position, int index, boolean value) {
      super(space, position, index);
      this.value = value;
    }

    public boolean getValue() {
      return value;
    }

    @Override public boolean addAll(List<Input> all) {
      throw new NoSuchElementException("Not supported for Input. It is a Leaf Elemet");
    }

    @Override public Input getElementByIndex(int index) {
      throw new NoSuchElementException("Not supported for Input. It is a Leaf Elemet");
    }

    @Override public boolean addElement(Input element) {
      throw new NoSuchElementException("Not supported for Input. It is a Leaf Elemet");
    }

    @Override public void removeElement(Input element) {
      throw new NoSuchElementException("Not supported for Input. It is a Leaf Elemet");
    }

    public void setValue(boolean sourceInput) {
      this.value = sourceInput;
    }
  }
}
