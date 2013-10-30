/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.space;


import java.awt.Dimension;
import java.awt.Point;

public class InputSpace extends BaseSpace<InputSpace.Input> {
  public InputSpace(int xSize, int ySize) {
    super(xSize, ySize);
  }

  public InputSpace(Dimension dimension) {
    super(dimension);
  }

  @Override
  protected Input createElement(BaseSpace<InputSpace.Input> space, int index, Point position) {
    return new Input(position, index, false);
  }

  public void setInputValue(int index, boolean value){
    this.getElementByIndex(index).setValue(value);
  }

  public boolean getInputValue(int index){
    return this.getElementByIndex(index).getValue();
  }


  public static class Input extends BaseSpace.Element{
     private boolean value;

    public Input(Point position, int index, boolean value) {
      super(position, index);
      this.value = value;
    }

    public boolean getValue() {
       return value;
     }

     public void setValue(boolean sourceInput) {
       this.value = sourceInput;
     }
   }
}
