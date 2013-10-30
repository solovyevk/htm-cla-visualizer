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

public class Column extends BaseSpace.Element {
  private static final Log LOG = LogFactory.getLog(Column.class);
  public static int CELLS_PER_COLUMN = 3;
  public static int AMOUNT_OF_PROXIMAL_SYNAPSES = 30;
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
    LOG.debug("Column X In Region Grid:" + position.x +"; In Input Space:" + inputSpaceX);
    LOG.debug("Column Y In Region Grid:" + position.y +"; In Input Space:" + inputSpaceY);
    return new Point(inputSpaceX, inputSpaceY);
  }

  public void createProximalSegment(InputSpace sensoryInput, double inputRadius){
    Point inputSpacePosition = this.getInputSpacePosition(sensoryInput);
    List<InputSpace.Input> potentialProximalInputs = sensoryInput.getAllWithinRadius(inputSpacePosition, inputRadius);
    Collections.shuffle(potentialProximalInputs);
    if(potentialProximalInputs.size() < AMOUNT_OF_PROXIMAL_SYNAPSES){
      throw new IllegalArgumentException("Amount of potential synapses:" +  AMOUNT_OF_PROXIMAL_SYNAPSES
                                         + " is bigger than number of inputs:" + potentialProximalInputs.size() +", increase input radius");
    }
    for (int j = 0; j < AMOUNT_OF_PROXIMAL_SYNAPSES; j++) {
      InputSpace.Input input = potentialProximalInputs.get(j);
      double permanence = .5;
      proximalSynapses.add(new Synapse.ProximalSynapse(permanence,input));
    }

    /*
    for (int i = 0; i < numSamples; i++)
    		{
    			// Get the current sample DataPoint.
    			inputPoint = InputSpaceArray[i];

    			double permanence = gausianNormalDistribution(generator);

    			// Distance from this column to the input bit, in the input space's coordinates.
    			dX = (Position.X * XSpace) - inputPoint.X;
    			dY = (Position.Y * YSpace) - inputPoint.Y;
    			distanceToInput_InputSpace = sqrt(dX * dX + dY * dY);

    			// Distance from this column to the input bit, in this Region's coordinates (used by Region::AverageReceptiveFieldSize()).
    			dX = Position.X - (inputPoint.X / XSpace);
    			dY = Position.Y - (inputPoint.Y / YSpace);
    			distanceToInput_RegionSpace = sqrt(dX * dX + dY * dY);

    			// Original
    			//double localityBias = RadiusBiasPeak / 2.0f * exp(pow(distanceToInput_InputSpace / (longerSide * RadiusBiasStandardDeviation), 2) / -2);
    			//double permanenceBias = Min(1.0f, permanence * localityBias);

    			// My version
    			double localityBias = pow(1.0 - Min(1.0, distanceToInput_InputSpace / (double)localityRadius), 0.001);
    			double permanenceBias = permanence * localityBias;

    			// Create the proximal synapse for the current sample.
    			ProximalSegment->CreateProximalSynapse(&(region->ProximalSynapseParams), curInput, inputPoint, permanenceBias, distanceToInput_RegionSpace);
    		}
     */

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
}
