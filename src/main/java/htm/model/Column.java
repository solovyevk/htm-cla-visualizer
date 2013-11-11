package htm.model;

import htm.model.space.BaseSpace;
import htm.model.space.InputSpace;
import htm.utils.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Column extends BaseSpace.Element {
  private static final Log LOG = LogFactory.getLog(Column.class);
  public static int CELLS_PER_COLUMN = 3;
  public static int AMOUNT_OF_PROXIMAL_SYNAPSES = 30;

  public static double BOOST_RATE = 0.01;
  /**
   * WP
   * A minimum number of inputs that must be active for a
   * column to be considered during the inhibition step.
   */
  public static int MIN_OVERLAP = 2;
  /**
   * WP
   * desiredLocalActivity A parameter controlling the number of columns that will be winners after the inhibition step.
   */
  public static int DESIRED_LOCAL_ACTIVITY = 2;

  //ProximalSynapse Parameters

  /**
   * WP
   * If the permanence value for a synapse is greater than this
   * value, it is said to be connected.
   */
  public static double CONNECTED_PERMANENCE = 0.2;
  /**
   * WP
   * Amount permanence values of synapses are incremented
   * during learning.
   */
  public static double PERMANENCE_INCREASE = 0.005;
  /**
   * WP
   * Amount permanence values of synapses are decremented
   * during learning.
   */
  public static double PERMANENCE_DECREASE = 0.005;
  /**
   * WP
   * overlap(c) The spatial pooler overlap of column c with a particular
   * input pattern.
   */
  private static final int COLUMN_CYCLE_BUFFER_SIZE = 1000;

  private static final CollectionUtils.Predicate<Synapse.ProximalSynapse> ACTIVE_CONNECTED_PROXIMAL_SYNAPSES_PREDICATE = new CollectionUtils.Predicate<Synapse.ProximalSynapse>() {
    @Override public boolean apply(Synapse.ProximalSynapse synapse) {
      return synapse.isConnected(CONNECTED_PERMANENCE) && synapse.getConnectedSensoryInput().getValue();
    }
  };

  private static final CollectionUtils.Predicate<Synapse.ProximalSynapse> CONNECTED_PROXIMAL_SYNAPSES_PREDICATE = new CollectionUtils.Predicate<Synapse.ProximalSynapse>() {
    @Override public boolean apply(Synapse.ProximalSynapse synapse) {
      return synapse.isConnected(CONNECTED_PERMANENCE);
    }
  };

  private static final Comparator<Column> OVERLAP_COMPARATOR = new Comparator<Column>() {
    @Override public int compare(Column column1, Column column2) {
      Double overlap1 = column1.getOverlap(), overlap2 = column2.getOverlap();
      return overlap2.compareTo(overlap1);
    }
  };

  private static final Comparator<Column> ACTIVE_DUTY_CYCLE_COMPARATOR = new Comparator<Column>() {
    @Override public int compare(Column column1, Column column2) {
      Double activeDutyCycle1 = column1.getActiveDutyCycle(), activeDutyCycle2 = column2.getActiveDutyCycle();
      return activeDutyCycle2.compareTo(activeDutyCycle1);
    }
  };

  private final Region region;
  private final List<Cell> cells = new ArrayList<Cell>();
  private final List<Synapse.ProximalSynapse> proximalSynapses = new ArrayList<Synapse.ProximalSynapse>();
  private int minimalOverlap = MIN_OVERLAP;
  private double boost = 1.0;


  private ColumnBufferedState<Double> overlap = new ColumnBufferedState<Double>(0.0) {
    @Override protected boolean positiveCondition(Double overlap) {
      return overlap >= minimalOverlap;
    }
  };

  private ColumnBufferedState<Boolean> active = new ColumnBufferedState<Boolean>(false) {
    @Override protected boolean positiveCondition(Boolean active) {
      return active;
    }
  };

  private Map<Double, List<Column>> neighbors_cache = new HashMap<Double, List<Column>>();

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

  public Point getInputSpacePosition() {
    return this.region.convertColumnPositionToInputSpace(this.getPosition());
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
    InputSpace sensoryInput = this.region.getInputSpace();
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
              distanceToInputColumn = BaseSpace.getDistance(this.region.convertInputPositionToColumnSpace(
                      input.getPosition()), position),
              initPermanence = PERMANENCE_INCREASE * randomGenerator.nextGaussian() + CONNECTED_PERMANENCE,
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
   * Phase 1: Overlap
   * Given an input vector, the first phase calculates the overlap of each column with that vector.
   * The overlap for each column is simply the number of connected synapses with active inputs,
   * multiplied by its boost. If this value is below minOverlap, we set the overlap score to zero.
   *
   * @return
   */
  public double computeOverlap() {
    double currentOverLap = getActiveConnectedSynapses().size();
    if (currentOverLap < minimalOverlap) {
      currentOverLap = 0;
    } else {
      currentOverLap = currentOverLap * getBoost();
    }
    overlap.addState(currentOverLap);
    return currentOverLap;
  }

  /**
   * WP
   * Phase 2: Inhibition
   * The second phase calculates which columns remain as winners after the inhibition step.
   * desiredLocalActivity is a parameter that controls the number of columns that end up winning.
   * For example, if desiredLocalActivity is 10, a column will be a winner if its overlap score is
   * greater than the score of the 10'th highest column within its inhibition radius.
   *
   * @param inhibitionRadius
   * @return
   */

  public boolean computeActiveDoInhibition(double inhibitionRadius) {
    double minLocalActivity = kthScore(getNeighbors(inhibitionRadius), DESIRED_LOCAL_ACTIVITY);
    this.active.addState(this.getOverlap() > 0 && this.getOverlap() >= minLocalActivity);
    return this.isActive();
  }

  /**
   * WP
   * Phase 3: Learning
   * The third phase performs learning; it updates the permanence values of all synapses as necessary, as well as the boost and inhibition radius.
   * The main learning rule is implemented in lines 20-26. For winning columns, if a synapse is active, its permanence value is incremented, otherwise it is decremented. Permanence values are constrained to be between 0 and 1.
   * Lines 28-36 implement boosting. There are two separate boosting mechanisms in place to help a column learn connections. If a column does not win often enough (as measured by activeDutyCycle), its overall boost value is increased (line 30-32). Alternatively, if a column's connected synapses do not overlap well with any inputs often enough (as measured by overlapDutyCycle), its permanence values are boosted (line 34-36). Note: once learning is turned off, boost(c) is frozen.
   * Finally, at the end of Phase 3 the inhibition radius is recomputed (line 38).
   */

  public void learnSpatial(double inhibitionRadius) {
    if (isActive()) {
      List<Synapse.ProximalSynapse> potentialSynapses = getPotentialSynapses();
      for (Synapse.ProximalSynapse potentialSynapse : potentialSynapses) {
        if (potentialSynapse.getConnectedSensoryInput().getValue()) {
          potentialSynapse.setPermanence(potentialSynapse.getPermanence() + Column.PERMANENCE_INCREASE);
        } else {
          potentialSynapse.setPermanence(potentialSynapse.getPermanence() - Column.PERMANENCE_DECREASE);
        }
      }
    }
    double minDutyCycle = 0.01 * getMaxDutyCycle(inhibitionRadius);
    updateBoost(minDutyCycle);
    if (this.getOverlapDutyCycle() < minDutyCycle) {
      increasePermanence(0.1 * CONNECTED_PERMANENCE);
    }
  }

  /**
   * WP
   * increasePermanence(c, s)
   * Increase the permanence value of every synapse in column c by a scale factor s.
   *
   * @param increaseBy
   */
  private void increasePermanence(double increaseBy) {
    List<Synapse.ProximalSynapse> proximalSynapses = this.getPotentialSynapses();
    for (Synapse.ProximalSynapse proximalSynapse : proximalSynapses) {
      proximalSynapse.setPermanence(proximalSynapse.getPermanence() + increaseBy);
    }

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
  private void updateBoost(double minDutyCycle) {
    if (this.getActiveDutyCycle() > minDutyCycle) {
      this.boost = 1.0;
    } else {
      this.boost += BOOST_RATE;
    }
  }

  /**
   * WP
   * kthScore(cols, k)
   * Given the list of columns, return the k'th highest overlap value.
   */
  private double kthScore(List<Column> neighbors, int desiredLocalActivity) {
    Collections.sort(neighbors, OVERLAP_COMPARATOR);
    if (desiredLocalActivity > neighbors.size()) {
      desiredLocalActivity = neighbors.size();
    }
    return neighbors.get(desiredLocalActivity - 1).getOverlap();

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
      result = region.getAllWithinRadius(this.getPosition(), roundedInhibitionRadius);
      //remove itself
      result.remove(result.indexOf(this));
      neighbors_cache.put(roundedInhibitionRadius, result);
    }
    return result;
  }

  public List<Synapse.ProximalSynapse> getActiveConnectedSynapses() {
    return CollectionUtils.filter(proximalSynapses, ACTIVE_CONNECTED_PROXIMAL_SYNAPSES_PREDICATE);
  }

  /**
  *WP
  *connectedSynapses(c)
  *A subset of potentialSynapses(c) where the permanence value is greater than connectedPerm.
  *These are the bottom-up inputs that are currently connected to column c.
  * @return
  */
  public List<Synapse.ProximalSynapse> getConnectedSynapses() {
    return CollectionUtils.filter(proximalSynapses, CONNECTED_PROXIMAL_SYNAPSES_PREDICATE);
  }

  /**
   * WP
   * potentialSynapses(c)
   * The list of potential synapses and their permanence values.
   * @return
   */
  public List<Synapse.ProximalSynapse> getPotentialSynapses() {
    return Collections.unmodifiableList(proximalSynapses);
  }

  public List<Cell> getCells() {
    return Collections.unmodifiableList(cells);
  }

  public Cell getCellByIndex(int index) {
    return cells.get(index);
  }


  public Region getRegion() {
    return region;
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
      double result = 0;
      int length = this.size(), activeCount = 0;
      for (int i = 0; i < length; i++) {
        if (positiveCondition(get(i))) {
          activeCount++;
        }
      }
      result = 1.0 * activeCount/length;
      return result;
    }

    protected abstract boolean positiveCondition(E state);
  }
}
