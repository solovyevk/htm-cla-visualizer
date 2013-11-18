package htm.visualizer.surface;

import htm.model.Column;
import htm.model.Synapse;
import htm.model.space.InputSpace;

import java.awt.*;
import java.util.List;

/*
This is our Sensory Input Interface, bits activated on mouse enter
 */
public class SensoryInputSurface extends BaseSurface.SquareElementsSurface {

  private final static Color LIGHT_BLUE = new Color(153, 204, 255);

  private final InputSpace sensoryInput;
  private Column currentColumn;
  private Integer selectedInputIndex;

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

  private void drawProximalSynapsesForColumn(Column column, Graphics2D g2d) {
    List<Synapse.ProximalSynapse> synapsesToDraw = column.getPotentialSynapses();
    Point center = column.getInputSpacePosition();
    Rectangle aroundRec = getElementAreaWithScale(center,
                                                  1 / (Math.PI / 4) * (sensoryInput.getShortSide() / column.getRegion().getShortSide()) * 1.1);
    g2d.setColor(Color.ORANGE);
    Composite original = g2d.getComposite();
    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                0.2f));
    g2d.fillOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
    g2d.setComposite(original);
    for (Synapse.ProximalSynapse proximalSynapse : synapsesToDraw) {
      Rectangle insideRec = getElementAreaWithScale(this.getElementPositionByIndex(
              proximalSynapse.getConnectedSensoryInput().getIndex()), .5);
      renderSynapse(g2d, proximalSynapse.getPermanence(), insideRec);
    }
    g2d.setComposite(original);
  }

  public static void renderSynapse(Graphics2D g2d, double permanence, Rectangle insideRec) {
    Composite original = g2d.getComposite();
    Color synapseStateColor = permanence >= Synapse.ProximalSynapse.CONNECTED_PERMANENCE ? Color.GREEN : Color.RED;
    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                Math.min(1.0f, Math.max(0.2f, (float)Math.abs(
                                                        permanence - Synapse.ProximalSynapse.CONNECTED_PERMANENCE) * 30))));
    g2d.setColor(synapseStateColor);
    g2d.fillRect(insideRec.x, insideRec.y, insideRec.width, insideRec.height);
    g2d.setComposite(original);
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
    if (currentColumn != null) {
      drawProximalSynapsesForColumn(currentColumn, g2d);
    }
    if (selectedInputIndex != null) {
      Composite original = g2d.getComposite();
      Rectangle aroundRec = getElementAreaWithScale(selectedInputIndex, 1 / (Math.PI / 4) * 1.5);
      g2d.setColor(LIGHT_BLUE);
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                  0.5f));
      g2d.fillOval(aroundRec.x, aroundRec.y, aroundRec.width, aroundRec.height);
      g2d.setComposite(original);
    }
  }

  public InputSpace getSensoryInput() {
    return sensoryInput;
  }

  public void setCurrentColumn(Column currentColumn) {
    this.currentColumn = this.currentColumn != currentColumn ? currentColumn : null;
    selectedInputIndex = null;
    this.repaint();
  }

  public void setSelectedInput(Integer inputIndex) {
    this.selectedInputIndex = inputIndex;
    this.repaint();
  }
}