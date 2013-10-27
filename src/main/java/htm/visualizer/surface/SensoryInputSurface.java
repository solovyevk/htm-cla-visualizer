package htm.visualizer.surface;

import java.awt.*;

/*
This is our Sensory Input Interface, bits activated on mouse enter
 */
public class SensoryInputSurface extends BaseSurface.SquareElementsSurface {

  private final boolean[] input;

  public SensoryInputSurface(int xSize, int ySize) {
    super(xSize, ySize);
    this.input = new boolean[xSize * ySize];
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
    g2d.setColor(activeColor);
    g2d.drawRect(x, y, width, height);
  }


  public void setInputValue(int index, boolean value) {
    input[index] = value;
    repaint(this.getElementAreaByIndex(index));
  }

  public boolean getInputValue(int index) {
    return input[index];
  }

  public void setSensoryInput(boolean[] source) {
    System.arraycopy(source, 0, input, 0, source.length);
    repaint();
  }

  public boolean[] getSensoryInput() {
    boolean[] result = new boolean[input.length];
    System.arraycopy(input, 0, result, 0, input.length);
    return result;
  }

  public void reset() {
    setSensoryInput(new boolean[xSize * ySize]);
  }

}