package htm.model;

import htm.model.algorithms.spatial.SpatialPooler;
import htm.model.algorithms.temporal.TemporalPooler;
import htm.model.space.BaseSpace;
import htm.model.space.InputSpace;
import htm.utils.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
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
  private SpatialPooler spatialPooler;

  public TemporalPooler getTemporalPooler() {
    return temporalPooler;
  }

  public void setTemporalPooler(TemporalPooler temporalPooler) {
    this.temporalPooler = temporalPooler;
  }

  public SpatialPooler getSpatialPooler() {
    return spatialPooler;
  }

  public void setSpatialPooler(SpatialPooler spatialPooler) {
    this.spatialPooler = spatialPooler;
  }


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

