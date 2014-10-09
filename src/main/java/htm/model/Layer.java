package htm.model;

import htm.model.algorithms.temporal.TemporalPooler;
import htm.model.space.BaseSpace;
import htm.model.space.InputSpace;
import htm.utils.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class Layer extends BaseSpace<Region, Column> {


  private final InputSpace inputSpace;
  /**
   * inputRadius for this input Space
   * The concept of Input Radius is an additional parameter to control how
   * far away synapse connections can be made instead of allowing connections anywhere.
   */
  private final double inputRadius;

  /**
   * Furthest number of columns away (in this Region's Column grid space) to allow new distal
   * synapse connections.  If set to 0 then there is no restriction and connections
   * can form between any two columns in the region.
   * <p/>
   * WP
   * <p/>
   * learningRadius The area around a temporal pooler cell from which it can get lateral connections.
   */
  private final double learningRadius;
  private final int cellsInColumn;

  private final boolean skipSpatial;

  private static final Log LOG = LogFactory.getLog(Layer.class);

  private static final CollectionUtils.Predicate<Column> BOTTOM_UP_WINNING_COLUMNS_PREDICATE = new CollectionUtils.Predicate<Column>() {
    @Override public boolean apply(Column column) {
      return column.isActive();
    }
  };

  //TODO not sure if Layer should directly reference algorithmic classes: Temporal/Spatial Pooler
  private TemporalPooler temporalPooler;

  public TemporalPooler getTemporalPooler() {
    return temporalPooler;
  }

  public void setTemporalPooler(TemporalPooler temporalPooler) {
    this.temporalPooler = temporalPooler;
  }

  private boolean spatialLearning = true;

  public Layer(Config layerCfg) {
    super(layerCfg.getRegionDimension().width, layerCfg.getRegionDimension().height);
    this.cellsInColumn = layerCfg.getCellsInColumn();
    this.initElementSpace();
    this.inputSpace = new InputSpace(layerCfg.getSensoryInputDimension().width,
                                     layerCfg.getSensoryInputDimension().height);
    this.inputRadius = layerCfg.getInputRadius();
    this.learningRadius = layerCfg.getLearningRadius();
    this.skipSpatial = layerCfg.isSkipSpatial();
    if (skipSpatial) {
      if (inputSpace.getDimension().height != this.getDimension().height || inputSpace.getDimension().width != this.getDimension().width) {
        throw new IllegalArgumentException(
                "With \"Skip Spatial Mode \" Sensory Input must be the same size as this Region");
      }
    } else {
      connectToInputSpace();
    }
  }

  @Override
  protected Column createElement(int index, Point position) {
    return new Column(this, index, position);
  }

  public java.util.List<Column> getColumns() {
    return this.getElements();
  }

  public void connectToInputSpace() {
    for (Column column : getColumns()) {
      column.createProximalSegment(inputRadius);
    }
  }

  public Point convertColumnPositionToInputSpace(Point columnPosition) {
    return convertPositionToOtherSpace(columnPosition, this.getDimension(), inputSpace.getDimension());
  }

  public Point convertInputPositionToColumnSpace(Point inputPosition) {
    return convertPositionToOtherSpace(inputPosition, inputSpace.getDimension(), this.getDimension());
  }

  public boolean getSpatialLearning() {
    return spatialLearning;
  }

  public void setSpatialLearning(boolean value) {
    this.spatialLearning = value;
  }


  /**
   * WP
   * activeColumns(t) t=0
   * List of column indices that are winners due to bottom-up input
   * (this is the output of the spatial pooler).
   *
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
   *
   * @param time (t - 0) - current step, (t - 1) - previous step, (t- n) - n step
   * @return
   */
  public List<Column> getActiveColumns(final int time) {
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
    double sum = 0;
    for (Column column : getColumns()) {
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
    return sum / getColumns().size();
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
    List<Column> regionColumns = getColumns();
    if (skipSpatial) {
      for (Column regionColumn : regionColumns) {
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
      if (getSpatialLearning()) {
        for (Column activeColumn : activeColumns) {
          activeColumn.learnSpatialForActive(inhibitionRadius);
        }
        for (Column column : regionColumns) {
          column.boostWeak(inhibitionRadius);
        }
      }
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

  public double getLearningRadius() {
    return learningRadius;
  }

  public boolean isSkipSpatial() {
    return skipSpatial;
  }

  public int getCellsInColumn() {
    return cellsInColumn;
  }

  public static class Config {
    private final Dimension regionDimension;
    private final Dimension sensoryInputDimension;
    private final double inputRadius;
    private final double learningRadius;
    private final boolean skipSpatial;
    private final int cellsInColumn;


    public Config(Dimension regionDimension, Dimension sensoryInputDimension,
                  double inputRadius, double learningRadius, boolean skipSpatial, int cellsInColumn) {
      this.regionDimension = regionDimension;
      this.sensoryInputDimension = sensoryInputDimension;
      this.inputRadius = inputRadius;
      this.learningRadius = learningRadius;
      this.skipSpatial = skipSpatial;
      this.cellsInColumn = cellsInColumn;
    }

    public double getLearningRadius() {
      return learningRadius;
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


    public int getCellsInColumn() {
      return cellsInColumn;
    }
  }
}

