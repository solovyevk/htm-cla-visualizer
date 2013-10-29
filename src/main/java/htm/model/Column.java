package htm.model;

import htm.model.space.BaseSpace;
import htm.model.space.InputSpace;
import htm.visualizer.utils.CircularArrayList;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Column extends BaseSpace.Element {
  public static int CELLS_PER_COLUMN = 3;
  private static final int COLUMN_MAX_ACTIVE = 1000;
  private final Region region;
  private final List<Cell> cells = new ArrayList<Cell>();
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
    double xSpace = sensoryInput.getDimension().width/region.getDimension().width;
    double ySpace = sensoryInput.getDimension().height/region.getDimension().height;
    Point result = new Point();
    result.setLocation(position.getX() * xSpace, position.getY() * ySpace);
    return result;
  }

  public void createProximalSegments(InputSpace sensoryInput, int inputRadius){
    int maxInputRadius = (int)Math.sqrt(Math.pow(sensoryInput.getDimension().width, 2) + Math.pow(sensoryInput.getDimension().height, 2));
    if(maxInputRadius < inputRadius){
      throw new IllegalArgumentException("Input Radius is bigger than maximum allowed: " + maxInputRadius);
    }
    double xSpace = sensoryInput.getDimension().width/region.getDimension().width;
    double ySpace = sensoryInput.getDimension().height/region.getDimension().height;
    //columnGridPosition.getX()

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
}
