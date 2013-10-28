package htm.visualizer.surface;

import htm.model.InputSpace;

import java.awt.*;

/*
This is our Sensory Input Interface, bits activated on mouse enter
 */
public class SensoryInputSurface extends BaseSurface.SquareElementsSurface {

  private final InputSpace sensoryInput;

  public SensoryInputSurface(int xSize, int ySize) {
    super(xSize, ySize);
    this.sensoryInput = new InputSpace(xSize, ySize);
    this.addElementMouseEnterListener(new ElementMouseEnterListener() {
      @Override public void onElementMouseEnter(ElementMouseEnterEvent e) {
        int index = e.getIndex();
        setInputValue(index, !getInputValue(index));
      }
    });
  }

  @Override
  protected void drawElement(Graphics2D g2d, int index, int x, int y, int width, int height) {
    g2d.setColor(getInputValue(index) ? activeColor : this.getBackground());
    g2d.fillRect(x, y, width, height);
    super.drawElement(g2d, index, x, y, width,
                      height);
  }


  public void setInputValue(int index, boolean value) {
    sensoryInput.setInput(index, value);
    repaint(this.getElementAreaByIndex(index));
  }

  public boolean getInputValue(int index) {
    return sensoryInput.getInput(index);
  }

  public void setSensoryInput(boolean[] source) {
    for (int i = 0; i < source.length; i++) {
      sensoryInput.setInput(i, source[i]);

    }
    repaint();
  }

  public boolean[] getSensoryInput() {
    boolean[] result = new boolean[dimension.width * dimension.height];
    for (int i = 0; i < result.length; i++) {
      result[i] = sensoryInput.getInput(i);

    }
    return result;
  }

  public void reset() {
    setSensoryInput(new boolean[dimension.width * dimension.height]);
  }

}