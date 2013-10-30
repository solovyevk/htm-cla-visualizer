package htm.visualizer.surface;

import htm.model.Column;
import htm.model.Synapse;
import htm.model.space.InputSpace;

import java.awt.*;

/*
This is our Sensory Input Interface, bits activated on mouse enter
 */
public class SensoryInputSurface extends BaseSurface.SquareElementsSurface {

  private final InputSpace sensoryInput;
  private Column currentColumn;

  public SensoryInputSurface(int xSize, int ySize) {
     this(new InputSpace(xSize, ySize));
  }

  public SensoryInputSurface(InputSpace sensoryInput) {
    super(sensoryInput.getDimension().width, sensoryInput.getDimension().height);
    this.sensoryInput = sensoryInput;
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

  public void drawProximalSynapsesForColumn(Column column, Graphics2D g2d){
    java.util.List<Synapse.ProximalSynapse> synapsesToDraw = column.getProximalSynapses();
    for (Synapse.ProximalSynapse proximalSynapse : synapsesToDraw) {
      Rectangle areaToDraw = this.getElementAreaByIndex(proximalSynapse.getConnectedSensoryInput().getIndex());
      //third of cell width
      int newWidth =  areaToDraw.width/3;
      g2d.setColor(Color.RED);
      g2d.fillRect(areaToDraw.x + newWidth + 1, areaToDraw.y + newWidth + 1, newWidth, newWidth);
    }
  }


  public void setInputValue(int index, boolean value) {
    sensoryInput.setInputValue(index, value);
    repaint(this.getElementAreaByIndex(index));
  }

  public boolean getInputValue(int index) {
    return sensoryInput.getInputValue(index);
  }

  public void setSensoryInputValues(boolean[] source) {
    for (int i = 0; i < source.length; i++) {
      sensoryInput.setInputValue(i, source[i]);

    }
    repaint();
  }

  public boolean[] getSensoryInputValues() {
    boolean[] result = new boolean[dimension.width * dimension.height];
    for (int i = 0; i < result.length; i++) {
      result[i] = sensoryInput.getInputValue(i);

    }
    return result;
  }

  public void reset() {
    setSensoryInputValues(new boolean[dimension.width * dimension.height]);
  }

  @Override protected void doDrawing(Graphics2D g2d) {
    super.doDrawing(g2d);
    if(currentColumn != null){
      drawProximalSynapsesForColumn(currentColumn, g2d);
    }
  }

  public InputSpace getSensoryInput() {
    return sensoryInput;
  }

  public void setCurrentColumn(Column currentColumn) {
    this.currentColumn = currentColumn;
  }
}