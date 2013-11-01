package htm.model;

import htm.model.space.BaseSpace;
import htm.model.space.InputSpace;
import htm.visualizer.utils.CircularArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Column extends BaseSpace.Element {
  private static final Log LOG = LogFactory.getLog(Column.class);
  public static int CELLS_PER_COLUMN = 3;
  public static int AMOUNT_OF_PROXIMAL_SYNAPSES = 30;
  /*
  *ProximalSynapse Parameters
   */
  public static double CONNECTED_PERMANENCE = 0.2;
  public static double PERMANENCE_INCREASE = 0.015;
  public static double PERMANENCE_DECREASE = 0.01;


  private static final int COLUMN_MAX_ACTIVE = 1000;

  private final Region region;
  private final List<Cell> cells = new ArrayList<Cell>();
  private final List<Synapse.ProximalSynapse> proximalSynapses = new ArrayList<Synapse.ProximalSynapse>();
  private ArrayList<Boolean> activeList = new CircularArrayList<Boolean>(COLUMN_MAX_ACTIVE);

  /**
   * activeDutyCycle(c) A sliding average representing how often column c has
   * been active after inhibition (e.g. over the last 1000 iterations).
   */
  private double activeDutyCycle;



  public Column(Region region, int columnIndex, Point columnGridPosition) {
    super(columnGridPosition, columnIndex);
    this.region = region;
    for (int i = 0; i < CELLS_PER_COLUMN; i++) {
      cells.add(new Cell(this, i));
    }
  }

  /*
  *A Point (srcX,srcY) of this Column's 'center' position in
  * terms of the proximal-synapse input space.
  **/

  public Point getInputSpacePosition(InputSpace sensoryInput){
    double xScale = sensoryInput.getDimension().getWidth()/region.getDimension().getWidth();
    double yScale = sensoryInput.getDimension().getHeight()/region.getDimension().getHeight();
    int inputSpaceX = Math.min((int) Math.ceil(position.getX() * xScale + (xScale > 1 ? xScale/2: 0)), sensoryInput.getDimension().width -1);
    int inputSpaceY = Math.min((int) Math.ceil(position.getY() * yScale + (yScale > 1 ? yScale/2: 0)),  sensoryInput.getDimension().height -1);
    return new Point(inputSpaceX, inputSpaceY);
  }

  public void createProximalSegment(InputSpace sensoryInput, double inputRadius){
    Point inputSpacePosition = this.getInputSpacePosition(sensoryInput);
    List<InputSpace.Input> potentialProximalInputs = sensoryInput.getAllWithinRadius(inputSpacePosition, inputRadius);
    if(potentialProximalInputs.size() < AMOUNT_OF_PROXIMAL_SYNAPSES){
        throw new IllegalArgumentException("Amount of potential synapses:" +  AMOUNT_OF_PROXIMAL_SYNAPSES
                                           + " is bigger than number of inputs:" + potentialProximalInputs.size() +", increase input radius");
      }
    Collections.shuffle(potentialProximalInputs);
    // Tie the random seed to this Column's position for reproducibility
    Random randomGenerator = new Random(this.getLocationSeed());
    for (int j = 0; j < AMOUNT_OF_PROXIMAL_SYNAPSES; j++) {
      InputSpace.Input input = potentialProximalInputs.get(j);
      //Permanence value is based on Gaussian distribution around the ConnectedPerm value, biased by distance from this Column.
      double distanceToInput = BaseSpace.getDistance(inputSpacePosition, input.getPosition()),
              initPermanence = PERMANENCE_INCREASE * randomGenerator.nextGaussian() + CONNECTED_PERMANENCE,
      radiusBiasDeviation = 0.1f, //1.1 t to 0.9
      radiusBiasScale = 1 + radiusBiasDeviation - radiusBiasDeviation/inputRadius * 2 * distanceToInput,
      radiusBiasPermanence =  initPermanence * radiusBiasScale;
      LOG.debug("PERMANENCE INITIALIZATION: init permanence" + initPermanence
                + " ,distanceToInput:" + distanceToInput + ", radiusBiasScale:" + radiusBiasScale + " , radiusBiasPermanence:" + radiusBiasPermanence);
      proximalSynapses.add(new Synapse.ProximalSynapse(radiusBiasPermanence,input));
    }
  }

  public List<Cell> getCells() {
    return Collections.unmodifiableList(cells);
  }

  public Cell getCellByIndex(int index) {
    return cells.get(index);
  }

  /**
   * updateActiveDutyCycle(c) Computes a moving average of how often column c
   * has been active after inhibition.
   *
   * @return
   */
  private double updateActiveDutyCycle() {
    int totalActive = 0;
    for (boolean act : activeList) {
      if (act) {
        totalActive++;
      }
    }
    this.activeDutyCycle = (double)totalActive / activeList.size();
    return activeDutyCycle;
  }

  public void setActive(boolean active) {
    // logger.log(Level.INFO, "activeList" + activeList.size());
    activeList.add(0, active);
    updateActiveDutyCycle();
  }

  public boolean isActive() {
    return this.activeList.size() > 0 ? activeList.get(0) : false;
  }

  public List<Synapse.ProximalSynapse> getProximalSynapses() {
    return Collections.unmodifiableList(proximalSynapses);
  }

  public Region getRegion() {
    return region;
  }
}
