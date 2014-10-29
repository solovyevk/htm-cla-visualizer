package htm.model;

import htm.model.space.BaseSpace;
import htm.model.space.Element;
import htm.model.space.InputSpace;
import htm.utils.CircularArrayList;
import htm.utils.CollectionUtils;
import htm.utils.MathUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Column extends Element<Layer, Cell> {
  private static final Log LOG = LogFactory.getLog(Column.class);
  public static int AMOUNT_OF_PROXIMAL_SYNAPSES = 30;

  private static final CollectionUtils.Predicate<Synapse.ProximalSynapse> ACTIVE_CONNECTED_PROXIMAL_SYNAPSES_PREDICATE = new CollectionUtils.Predicate<Synapse.ProximalSynapse>() {
    @Override
    public boolean apply(Synapse.ProximalSynapse synapse) {
      return synapse.isConnected(
              Synapse.ProximalSynapse.CONNECTED_PERMANENCE) && synapse.getConnectedSensoryInput().getValue();
    }
  };

  private static final CollectionUtils.Predicate<Synapse.ProximalSynapse> CONNECTED_PROXIMAL_SYNAPSES_PREDICATE = new CollectionUtils.Predicate<Synapse.ProximalSynapse>() {
    @Override
    public boolean apply(Synapse.ProximalSynapse synapse) {
      return synapse.isConnected(Synapse.ProximalSynapse.CONNECTED_PERMANENCE);
    }
  };

  private static final Comparator<Column> ACTIVE_DUTY_CYCLE_COMPARATOR = new Comparator<Column>() {
    @Override public int compare(Column column1, Column column2) {
      Double activeDutyCycle1 = column1.getActiveDutyCycle(), activeDutyCycle2 = column2.getActiveDutyCycle();
      return activeDutyCycle2.compareTo(activeDutyCycle1);
    }
  };


  @Override
  public boolean addAll(List<Cell> all) {
    throw new NoSuchElementException("Not supported for Column, fixed number of cells");
  }

  @Override
  public boolean addElement(Cell element) {
    throw new NoSuchElementException("Not supported for Column, fixed number of cells");
  }

  @Override
  public void removeElement(Cell element) {
    throw new NoSuchElementException("Not supported for Column, fixed number of cells");
  }

  /**
   * WP
   * overlap(c) The spatial pooler overlap of column c with a particular
   * input pattern.
   */
  private static final int COLUMN_CYCLE_BUFFER_SIZE = 1000;


  public static void updateFromConfig(Config columnCfg) {
    Column.AMOUNT_OF_PROXIMAL_SYNAPSES = columnCfg.getAmountOfProximalSynapses();
  }


  private final List<Synapse.ProximalSynapse> proximalSynapses = new ArrayList<Synapse.ProximalSynapse>();
  private double boost = 1.0;


  private ColumnBufferedState<Double> overlap; //Need to create it later in constructor to access Column instance

  private ColumnBufferedState<Boolean> active = new ColumnBufferedState<Boolean>(false) {
    @Override protected boolean positiveCondition(Boolean active) {
      return active;
    }
  };

  private Map<Double, List<Column>> neighbors_cache = new HashMap<Double, List<Column>>();

  public Column(final Layer layer, int columnIndex, Point columnGridPosition) {
    super(layer, columnGridPosition, columnIndex);
    for (int i = 0; i < layer.getCellsInColumn(); i++) {
      this.elementList.add(new Cell(this, i));
    }


    overlap = new ColumnBufferedState<Double>(0.0) {

      @Override protected boolean positiveCondition(Double overlap) {
         //TODO not sure if Column should directly reference algorithmic classes: Temporal/Spatial Pooler,
         //TODO but having TP properties in column as static prop even worse
        if(layer.getSpatialPooler() == null){
          throw new RuntimeException("Spatial Pooler should be defined");
        }
        return overlap >= layer.getSpatialPooler().getMinimalOverlap();
      }
    };
  }

  public void updateOverlap(double currentOverLap) {
    overlap.addState(currentOverLap);
  }


  /*
  *A Point (srcX,srcY) of this Column's 'center' position in
  * terms of the proximal-synapse input space.
  **/

  public Point getInputSpacePosition() {
    return this.getOwner().convertColumnPositionToInputSpace(this.getPosition());
  }

  /**
   * WP
   * Prior to receiving any inputs, the region is initialized by computing a list of initial
   * potential synapses for each column. This consists of a random set of inputs selected
   * from the input space. Each input is represented by a synapse and assigned a
   * random permanence value. The random permanence values are chosen with two
   * criteria. First, the values are chosen to be in a small range around connectedPerm
   * (the minimum permanence value at which a synapse is considered "connected").
   * This enables potential synapses to become connected (or disconnected) after a
   * small number of training iterations. Second, each column has a natural center over
   * the input region, and the permanence values have a bias towards this center (they
   * have higher values near the center).
   *
   * @param inputRadius
   */

  public void createProximalSegment(double inputRadius) {
    InputSpace sensoryInput = this.getOwner().getInputSpace();
    Point inputSpacePosition = this.getInputSpacePosition();
    List<InputSpace.Input> potentialProximalInputs = sensoryInput.getAllWithinRadius(inputSpacePosition, inputRadius);
    if (potentialProximalInputs.size() < AMOUNT_OF_PROXIMAL_SYNAPSES) {
      throw new IllegalArgumentException("Amount of potential synapses:" + AMOUNT_OF_PROXIMAL_SYNAPSES
                                         + " is bigger than number of inputs:" + potentialProximalInputs.size() + ", increase input radius");
    }
    Collections.shuffle(potentialProximalInputs);
    // Tie the random seed to this Column's position for reproducibility
    inputRadius = inputRadius < 1 ? Math.sqrt(Math.pow(sensoryInput.getDimension().height, 2) + Math.pow(
            sensoryInput.getDimension().width, 2)) : inputRadius;
    Random randomGenerator = new Random(this.getLocationSeed());
    for (int j = 0; j < AMOUNT_OF_PROXIMAL_SYNAPSES; j++) {
      InputSpace.Input input = potentialProximalInputs.get(j);
      //Permanence value is based on Gaussian distribution around the ConnectedPerm value, biased by distance from this Column.
      double distanceToInputSrc = BaseSpace.getDistance(inputSpacePosition, input.getPosition()),
              distanceToInputColumn = BaseSpace.getDistance(this.getOwner().convertInputPositionToColumnSpace(
                      input.getPosition()), position),
              initPermanence = Synapse.ProximalSynapse.PERMANENCE_INCREASE * randomGenerator.nextGaussian() + Synapse.ProximalSynapse.CONNECTED_PERMANENCE,
              radiusBiasDeviation = 0.1f, //1.1 to 0.9 -> Y = 1.1 - radiusBiasDeviation / 2inputRadius * X
              radiusBiasScale = 1 + radiusBiasDeviation - radiusBiasDeviation / inputRadius * 2 * distanceToInputSrc,
              radiusBiasPermanence = initPermanence * radiusBiasScale;
      LOG.debug("PERMANENCE INITIALIZATION: init permanence" + initPermanence
                + " ,distanceToInputSrc:" + distanceToInputSrc + ", distanceToInputColumn:" + distanceToInputColumn + ", radiusBiasScale:" + radiusBiasScale + " , radiusBiasPermanence:" + radiusBiasPermanence);
      proximalSynapses.add(new Synapse.ProximalSynapse(radiusBiasPermanence, input, this));
    }
  }

  /**
   * WP
   * increasePermanence(c, s)
   * Increase the permanence value of every synapse in column c by a scale factor s.
   *
   * @param increaseBy
   */
  public void increasePermanence(double increaseBy) {
    //List<Synapse.ProximalSynapse> proximalSynapses = this.getPotentialSynapses();
    for (Synapse.ProximalSynapse proximalSynapse : proximalSynapses) {
      proximalSynapse.setPermanence(proximalSynapse.getPermanence() + increaseBy);
    }

  }


  /**
   * overlap(c) The spatial pooler overlap of column c with a particular
   * input pattern.
   *
   * @return
   */

  public double getOverlap() {
    return overlap.getLast();
  }

  /**
   * WP
   * overlapDutyCycle(c) A sliding average representing how often column c has had
   * significant overlap (i.e. greater than minOverlap) with its
   * inputs (e.g. over the last 1000 iterations).
   *
   * @return
   */

  public double getOverlapDutyCycle() {
    return overlap.getSlidingAverage();
  }

  public boolean isActive() {
    return active.getLast();
  }


  /**
   * Get column active state at time
   *
   * @param time (t - 0) - current step, (t - 1) - previous step, (t- n) - n step
   * @return
   */
  public boolean isActive(int time) {
    if (time > active.size()) {
      throw new IllegalArgumentException("time: " + time + " can't exceed history buffer limit: " + active.size());
    }
    return active.get(time);
  }


  public void setActive(boolean active) {
    this.active.addState(active);
  }

  /**
   * WP
   * activeDutyCycle(c) A sliding average representing how often column c has
   * been active after inhibition (e.g. over the last 1000 iterations).
   */
  public double getActiveDutyCycle() {
    return active.getSlidingAverage();
  }

  /**
   * maxDutyCycle(cols)
   * Returns the maximum active duty cycle of the columns in the given list of columns.
   *
   * @param inhibitionRadius
   * @return
   */
  public double getMaxDutyCycle(double inhibitionRadius) {
    List<Column> neighbors = this.getNeighbors(inhibitionRadius);
    Collections.sort(neighbors, ACTIVE_DUTY_CYCLE_COMPARATOR);
    return neighbors.get(0).getActiveDutyCycle();
  }

  /*
  * WP
  * The boost value for column c as computed during learning -
  * used to increase the overlap value for inactive columns.
  */
  public double getBoost() {
    return boost;
  }


  /**
   * WP
   * boostFunction(c)
   * Returns the boost value of a column. The boost value is a scalar >= 1.
   * If activeDutyCycle(c) is above minDutyCycle(c), the boost value is 1.
   * The boost increases linearly once the column's activeDutyCyle starts falling below its minDutyCycle.
   *
   * @param minDutyCycle
   * @return
   */
  public void updateBoost(double minDutyCycle, double boostRate) {
    if (this.getActiveDutyCycle() > minDutyCycle) {
      this.boost = 1.0;
    } else {
      this.boost += boostRate;
    }
  }

  /*
  * WP
  * neighbors(c) A list of all the columns that are within inhibitionRadius of
  * column c.
  */
  public List<Column> getNeighbors(Double inhibitionRadius) {
    //to limit cache to 0.1 fraction
    Double roundedInhibitionRadius = MathUtils.round(inhibitionRadius, 1);
    List<Column> result;
    result = neighbors_cache.get(roundedInhibitionRadius);
    if (result == null) {
      result = this.getOwner().getAllWithinRadius(this.getPosition(), roundedInhibitionRadius);
      //remove itself
      result.remove(result.indexOf(this));
      if (result.size() == 0) {
        throw new IllegalArgumentException(
                "No neighbors found within inhibitionRadius of: " + inhibitionRadius + ". Please increase receptiveFieldSize by increasing inputRadius for input Space.");
      }
      neighbors_cache.put(roundedInhibitionRadius, result);
    }
    return result;
  }

  public List<Synapse.ProximalSynapse> getActiveConnectedSynapses() {
    return CollectionUtils.filter(proximalSynapses, ACTIVE_CONNECTED_PROXIMAL_SYNAPSES_PREDICATE);
  }

  /**
   * WP
   * connectedSynapses(c)
   * A subset of potentialSynapses(c) where the permanence value is greater than connectedPerm.
   * These are the bottom-up inputs that are currently connected to column c.
   *
   * @return
   */
  public List<Synapse.ProximalSynapse> getConnectedSynapses() {
    return CollectionUtils.filter(proximalSynapses, CONNECTED_PROXIMAL_SYNAPSES_PREDICATE);
  }

  /**
   * WP
   * potentialSynapses(c)
   * The list of potential synapses and their permanence values.
   *
   * @return
   */
  public List<Synapse.ProximalSynapse> getPotentialSynapses() {
    return Collections.unmodifiableList(proximalSynapses);
  }


  public void nextTimeStep() {
    for (Cell cell : this.getElementsList()) {
      cell.nextTimeStep();
    }
  }


  private static abstract class ColumnBufferedState<E> extends CircularArrayList<E> {
    public ColumnBufferedState(E defValue) {
      super(COLUMN_CYCLE_BUFFER_SIZE);
      addState(defValue);
    }

    /**
     * Set the column state
     *
     * @param value
     */
    void addState(E value) {
      this.add(0, value);
    }

    public E getLast() {
      return this.get(0);
    }

    public double getSlidingAverage() {
      double result;
      int length = this.size(), activeCount = 0;
      for (int i = 0; i < length; i++) {
        if (positiveCondition(get(i))) {
          activeCount++;
        }
      }
      result = 1.0 * activeCount / length;
      return result;
    }

    protected abstract boolean positiveCondition(E state);
  }

  public static class Config {
    private final int amountOfProximalSynapses;
    public Config(int amountOfProximalSynapses) {
      this.amountOfProximalSynapses = amountOfProximalSynapses;
    }

    public int getAmountOfProximalSynapses() {
      return amountOfProximalSynapses;
    }
  }
}
