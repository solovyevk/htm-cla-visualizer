

package htm.model;


import htm.visualizer.utils.CollectionUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InputSpace {
  private final List<Input> inputList;
  private final Dimension dimension;


  public InputSpace(int xSize, int ySize) {
    this.dimension = new Dimension(xSize, ySize);
    inputList = new ArrayList<Input>(xSize * ySize);
    int index = 0;
    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        inputList.add(index, new Input(index, new Point(x, y), false));
        index++;
      }
    }
  }

  public InputSpace(Dimension dimension) {
    this(dimension.width, dimension.height);
  }


  public void setInput(int index, boolean input){
    inputList.get(index).setValue(input);
  }

  public boolean getInput(int index){
    return inputList.get(index).getValue();
  }

  public Dimension getDimension() {
    return dimension;
  }

  public Collection<Input> getInputWithinRadius(final Point center, final double radius){
    CollectionUtils.Predicate<Input> withinRadius = new CollectionUtils.Predicate<Input>() {
      @Override public boolean apply(Input input) {
        return Math.pow(center.x - input.getPosition().x, 2) + Math.pow(center.y - input.getPosition().y,2) < Math.pow(radius, 2);
      }
    };
    return CollectionUtils.filter(inputList, withinRadius);
  }


  public static class Input {
    private final Point position;
    private final int inputSpaceIndex;
    private boolean value;

    public Input(int index, Point position, boolean value) {
      this.inputSpaceIndex = index;
      this.position = position;
      this.value = value;
    }

    public Point getPosition() {
      return position;
    }


    public boolean getValue() {
      return value;
    }

    public void setValue(boolean sourceInput) {
      this.value = sourceInput;
    }

    public int getInputSpaceIndex() {
      return inputSpaceIndex;
    }
  }
}
