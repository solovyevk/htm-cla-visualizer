package htm.model;

import htm.model.space.ColumnSpace;
import htm.model.space.InputSpace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class Region extends ColumnSpace {
  private final InputSpace inputSpace;
  private final double inputRadius;

  private static final Log LOG = LogFactory.getLog(Region.class);


  public Region(int xSize, int ySize, InputSpace source, double inputRadius) {
    super(xSize, ySize);
    this.inputSpace = source;
    this.inputRadius = inputRadius;
    connectToInputSpace();
  }

  public Region(int xSize, int ySize, InputSpace source) {
    this(xSize, ySize, source, -1);
  }

  public void connectToInputSpace() {
    Column[] columns = this.getColumns();
    for (Column column : columns) {
      column.createProximalSegment(inputRadius);
    }
  }

  public Point convertColumnPositionToInputSpace(Point columnPosition) {
    return convertPositionToOtherSpace(columnPosition, this.getDimension(), inputSpace.getDimension());
  }

  public Point convertInputPositionToColumnSpace(Point inputPosition) {
    return convertPositionToOtherSpace(inputPosition, inputSpace.getDimension(), this.getDimension());
  }

  /**
   * WP
   * averageReceptiveFieldSize()
   * The radius of the average connected receptive field size of all the columns.
   * The connected receptive field size of a column includes only the connected synapses (those with permanence values >= connectedPerm).
   * This is used to determine the extent of lateral inhibition between columns.
   */

  public double getAverageReceptiveFieldSize() {
    Column[] columns = this.getColumns();
    double sum = 0;
    for (Column column : columns) {
      java.util.List<Synapse.ProximalSynapse> connectedSynapses = column.getConnectedSynapses();
      double maxDistance = 0;
      for (Synapse.ProximalSynapse connectedSynapse : connectedSynapses) {
        // Determine the distance of the further proximal synapse. This will be considered the size of the receptive field.
        double distanceToInput = connectedSynapse.getDistanceToColumn();
        maxDistance = Math.max(maxDistance, connectedSynapse.getDistanceToColumn());
      }
      LOG.debug("maxDistance for column:#" + column.getIndex() + " - " + maxDistance);
      // Add the current column's receptive field size to the sum.
      sum += maxDistance;
    }
    return sum / columns.length;
  }

  /**
   * Performs spatial pooling for the current input in this Region.
   * The result will be a subset of Columns being set as active as well
   * as (proximal) synapses in all Columns having updated permanences and boosts, and
   * the Region will update inhibitionRadius.
   *
   * WP
   * Phase 1:
   * Compute the overlap with the current input for each column. Given an input
   * vector, the first phase calculates the overlap of each column with that
   * vector. The overlap for each column is simply the number of connected
   * synapses with active inputs, multiplied by its boost. If this value is
   * below minOverlap, we set the overlap score to zero.
   *
   * Phase 2:
   * Compute the winning columns after inhibition. The second phase calculates
   * which columns remain as winners after the inhibition step.
   * desiredLocalActivity is a parameter that controls the number of columns
   * that end up winning. For example, if desiredLocalActivity is 10, a column
   * will be a winner if its overlap score is greater than the score of the
   * 10'th highest column within its inhibition radius.
   *
   * Phase 3:
   * Update synapse permanence and internal variables.The third phase performs
   * learning; it updates the permanence values of all synapses as necessary,
   * as well as the boost and inhibition radius. The main learning rule is
   * implemented in lines 20-26. For winning columns, if a synapse is active,
   * its permanence value is incremented, otherwise it is decremented. Permanence
   * values are constrained to be between 0 and 1.
   * Lines 28-36 implement boosting. There are two separate boosting mechanisms
   * in place to help a column learn connections. If a column does not win often
   * enough (as measured by activeDutyCycle), its overall boost value is
   * increased (line 30-32). Alternatively, if a column's connected synapses do
   * not overlap well with any inputs often enough (as measured by
   * overlapDutyCycle), its permanence values are boosted (line 34-36).
   */


  public void performSpatialPooling() {
    double inhibitionRadius = getAverageReceptiveFieldSize();
    Column[] regionColumns = getColumns();
    List<Column> activeColumns = new ArrayList<Column>();
    //Phase 1: Compute the overlap
    for (Column column : regionColumns) {
       column.computeOverlap();
    }
    //Phase 2:Compute the winning columns after inhibition
    for (Column column : regionColumns) {
       if(column.computeActiveDoInhibition(inhibitionRadius)){
         activeColumns.add(column);
       }
    }
    // Phase 3: Update synapse permanence and internal variables
    for (Column activeColumn : activeColumns) {
      activeColumn.learnSpatialForActive(inhibitionRadius);
    }
    for (Column column : regionColumns) {
       column.boostWeak(inhibitionRadius);
    }
  }


  public InputSpace getInputSpace() {
    return inputSpace;
  }

  public double getInputRadius() {
    return inputRadius;
  }
}

