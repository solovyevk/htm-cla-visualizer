package htm.model;

import htm.model.space.ColumnSpace;
import htm.model.space.InputSpace;
import htm.utils.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class Region extends ColumnSpace {
  private final InputSpace inputSpace;
  /**
   * inputRadius for this input Space
   * The concept of Input Radius is an additional parameter to control how
   * far away synapse connections can be made instead of allowing connections anywhere.
   */
  private final double inputRadius;

  /**
  *Furthest number of columns away (in this Region's Column grid space) to allow new distal
  *synapse connections.  If set to 0 then there is no restriction and connections
  *can form between any two columns in the region.

  *WP
  *
  *learningRadius The area around a temporal pooler cell from which it can get lateral connections.
  */
  private final double learningRadius = 4.0;


  private final boolean skipSpatial;

  private static final Log LOG = LogFactory.getLog(Region.class);

  private static final CollectionUtils.Predicate<Column> BOTTOM_UP_WINNING_COLUMNS_PREDICATE = new CollectionUtils.Predicate<Column>() {
    @Override public boolean apply(Column column) {
      return column.isActive();
    }
  };

  public Region(Config regionCfg) {
    super(regionCfg.getRegionDimension().width, regionCfg.getRegionDimension().height);
    this.inputSpace = new InputSpace(regionCfg.getSensoryInputDimension().width,
                                     regionCfg.getSensoryInputDimension().height);
    this.inputRadius = regionCfg.getInputRadius();
    this.skipSpatial = regionCfg.isSkipSpatial();
    if (skipSpatial) {
      if (inputSpace.getDimension().height != this.getDimension().height || inputSpace.getDimension().width != this.getDimension().width) {
        throw new IllegalArgumentException(
                "With \"Skip Spatial Mode \" Sensory Input must be the same size as this Region");
      }
    } else {
      connectToInputSpace();
    }
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

  public boolean getTemporalLearning(){
    return true;
  }

  /**
   * WP
   * activeColumns(t) t=0
   * List of column indices that are winners due to bottom-up input
   * (this is the output of the spatial pooler).
   * @return
   */

  public List<Column> getActiveColumns() {
     return CollectionUtils.filter(this.getElements(), BOTTOM_UP_WINNING_COLUMNS_PREDICATE);
   }

  /**
  * WP
  * activeColumns(t)
  * List of column indices that are winners due to bottom-up input
  * (this is the output of the spatial pooler).
   * @param time (t - 0) - current step, (t - 1) - previous step, (t- n) - n step
   * @return
   */
  public List<Column> getActiveColumns(final int time){
    return CollectionUtils.filter(this.getElements(), new CollectionUtils.Predicate<Column>() {
      @Override public boolean apply(Column column) {
        return column.isActive(time);
      }
    });
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
   * <p/>
   * WP
   * Phase 1:
   * Compute the overlap with the current input for each column. Given an input
   * vector, the first phase calculates the overlap of each column with that
   * vector. The overlap for each column is simply the number of connected
   * synapses with active inputs, multiplied by its boost. If this value is
   * below minOverlap, we set the overlap score to zero.
   * <p/>
   * Phase 2:
   * Compute the winning columns after inhibition. The second phase calculates
   * which columns remain as winners after the inhibition step.
   * desiredLocalActivity is a parameter that controls the number of columns
   * that end up winning. For example, if desiredLocalActivity is 10, a column
   * will be a winner if its overlap score is greater than the score of the
   * 10'th highest column within its inhibition radius.
   * <p/>
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
    if (skipSpatial) {
      for (int i = 0; i < regionColumns.length; i++) {
        Column regionColumn = regionColumns[i];
        regionColumn.setActive(inputSpace.getInputValue(regionColumn.getIndex()));
      }
    } else {
      List<Column> activeColumns = new ArrayList<Column>();
      //Phase 1: Compute the overlap
      for (Column column : regionColumns) {
        column.computeOverlap();
      }
      //Phase 2:Compute the winning columns after inhibition
      for (Column column : regionColumns) {
        if (column.computeActiveDoInhibition(inhibitionRadius)) {
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
  }

  /**
   * Performs temporal pooling based on the current spatial pooler output.
   * WP:
   * The input to this code is activeColumns(t), as computed by the spatial pooler.
   * The code computes the active and predictive state for each cell at the current
   * time step, t. The boolean OR of the active and predictive states for each cell
   * forms the output of the temporal pooler for the next level.
   * <p/>
   * Phase 1:
   * Compute the active state, activeState(t), for each cell.
   * The first phase calculates the activeState for each cell that is in a winning column.
   * For those columns, the code further selects one cell per column as the learning cell (learnState).
   * The logic is as follows: if the bottom-up input was predicted by any cell
   * (i.e. its predictiveState output was 1 due to a sequence segment),
   * then those cells become active (lines 23-27). If that segment became
   * active from cells chosen with learnState on, this cell is selected as the learning cell (lines 28-30).
   * If the bottom-up input was not predicted, then all cells in the become active (lines 32-34).
   * In addition, the best matching cell is chosen as the learning cell (lines 36-41) and a
   * new segment is added to that cell.
   */
  public void performTemporalPooling() {
    //Phase 1:Compute the active state, activeState(t), for each cell.
    List<Column> activeColumns = this.getActiveColumns();
    for (Column activeColumn : activeColumns) {

    }


  }

  public Dimension getInputSpaceDimension() {
    return inputSpace.getDimension();
  }

  public InputSpace getInputSpace() {
    return inputSpace;
  }

  public double getInputRadius() {
    return inputRadius;
  }

  public double getLearningRadius(){
    return learningRadius;
  }

  public boolean isSkipSpatial() {
    return skipSpatial;
  }

  public static class Config {
    private final Dimension regionDimension;
    private final Dimension sensoryInputDimension;
    private final double inputRadius;
    private final boolean skipSpatial;


    public Config(Dimension regionDimension, Dimension sensoryInputDimension,
                  double inputRadius, boolean skipSpatial) {
      this.regionDimension = regionDimension;
      this.sensoryInputDimension = sensoryInputDimension;
      this.inputRadius = inputRadius;
      this.skipSpatial = skipSpatial;
    }


    public double getInputRadius() {
      return inputRadius;
    }

    public Dimension getRegionDimension() {
      return regionDimension;
    }

    public Dimension getSensoryInputDimension() {
      return sensoryInputDimension;
    }

    public boolean isSkipSpatial() {
      return skipSpatial;
    }

  }
}

