package htm.model;

import htm.model.space.BaseSpace;
import htm.model.space.InputSpace;
import htm.utils.CircularArrayList;
import htm.utils.CollectionUtils;
import htm.utils.MathUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Column extends BaseSpace.Element {
  private static final Log LOG = LogFactory.getLog(Column.class);
  public static int AMOUNT_OF_PROXIMAL_SYNAPSES = 30;
  /**
   * The amount that is added to a Column's Boost value in a single time step, when it is being boosted.
   */
  public static double BOOST_RATE = 0.01;
  /**
   * WP
   * A minimum number of inputs that must be active for a
   * column to be considered during the inhibition step.
   */
  public static int MIN_OVERLAP = 2;
  /**
   * WP
   * desiredLocalActivity A parameter controlling the number of columns that
   * will be winners after the inhibition step.
   */
  public static int DESIRED_LOCAL_ACTIVITY = 2;


  /**
   * WP
   * overlap(c) The spatial pooler overlap of column c with a particular
   * input pattern.
   */
  private static final int COLUMN_CYCLE_BUFFER_SIZE = 1000;

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

  public static void updateFromConfig(Config columnCfg) {
    Column.AMOUNT_OF_PROXIMAL_SYNAPSES = columnCfg.getAmountOfProximalSynapses();
    Column.MIN_OVERLAP = columnCfg.getMinOverlap();
    Column.DESIRED_LOCAL_ACTIVITY = columnCfg.getDesiredLocalActivity();
    Column.BOOST_RATE = columnCfg.getBoostRate();
  }


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
    for (int i = 0; i < region.getCellsInColumn(); i++) {
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
   * SPATIAL POOLING
   * <p/>
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
   * SPATIAL POOLING
   * <p/>
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
    this.setActive(this.getOverlap() > 0 && this.getOverlap() >= minLocalActivity);
    return this.isActive();
  }

  /**
   * WP
   * SPATIAL POOLING
   * <p/>
   * Phase 3: Learning
   * The third phase performs learning; it updates the permanence values of all synapses as necessary, as well as the boost and inhibition radius.
   * <p/>
   * First part
   * The main learning rule is implemented in lines 20-26. For winning columns, if a synapse is active, its permanence value is incremented, otherwise it is decremented. Permanence values are constrained to be between 0 and 1.
   *
   * @param inhibitionRadius
   */

  public void learnSpatialForActive(double inhibitionRadius) {
    if (isActive()) {
      List<Synapse.ProximalSynapse> potentialSynapses = getPotentialSynapses();
      for (Synapse.ProximalSynapse potentialSynapse : potentialSynapses) {
        if (potentialSynapse.getConnectedSensoryInput().getValue()) {
          potentialSynapse.setPermanence(
                  potentialSynapse.getPermanence() + Synapse.ProximalSynapse.PERMANENCE_INCREASE);
        } else {
          potentialSynapse.setPermanence(
                  potentialSynapse.getPermanence() - Synapse.ProximalSynapse.PERMANENCE_DECREASE);
        }
      }
    }
  }

  /**
   * Second part
   * Lines 28-36 implement boosting. There are two separate boosting mechanisms in place to help a column learn connections. If a column does not win often enough (as measured by activeDutyCycle), its overall boost value is increased (line 30-32). Alternatively, if a column's connected synapses do not overlap well with any inputs often enough (as measured by overlapDutyCycle), its permanence values are boosted (line 34-36). Note: once learning is turned off, boost(c) is frozen.
   * Finally, at the end of Phase 3 the inhibition radius is recomputed (line 38).
   *
   * @param inhibitionRadius
   */
  public void boostWeak(double inhibitionRadius) {
    double minDutyCycle = 0.01 * getMaxDutyCycle(inhibitionRadius);
    updateBoost(minDutyCycle);
    if (this.getOverlapDutyCycle() < minDutyCycle) {
      increasePermanence(0.1 * Synapse.ProximalSynapse.CONNECTED_PERMANENCE);
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
   * Number of step until column will be predicted in temporal sequence/step(s)
   *
   * @return
   */
  public int isPredictInStep() {
    for (Cell cell : cells) {
      if (cell.getPredictiveState(Cell.NOW)) {
        return cell.getPredictInStepState(Cell.NOW);
      }
    }
    return Cell.NOT_IN_STEP_PREDICTION;
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


  void setActive(boolean active) {
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

  /**
   * WP
   * TEMPORAL POOLING
   * <p/>
   * Phase 1: Compute cells state
   * The first phase calculates the activeState for each cell that is in a winning column.
   * For those columns, the code further selects one cell per column as the learning cell (learnState).
   * The logic is as follows: if the bottom-up input was predicted by any cell (i.e. its predictiveState
   * output was 1 due to a sequence segment), then those cells become active (lines 23-27).
   * If that segment became active from cells chosen with learnState on, this cell is selected as
   * the learning cell (lines 28-30). If the bottom-up input was not predicted, then all cells
   * in the become active (lines 32-34). In addition, the best matching cell is chosen as the
   * learning cell (lines 36-41) and a new segment is added to that cell.
   */
  public void computeCellsActiveState() {
    if (!isActive()) {
      throw new RuntimeException("Column should be active");
    }
    boolean buPredicted = false, lcChosen = false;
    for (Cell cell : cells) {
      if (cell.getPredictiveState(Cell.BEFORE)) {
        DistalDendriteSegment segment = cell.getActiveSegment(Cell.BEFORE, Cell.State.ACTIVE);
        if (segment != null && segment.isSequenceSegment()) {
          buPredicted = true;
          cell.setActiveState(true);
          if (segment.segmentActive(Cell.BEFORE, Cell.State.LEARN)) {
            lcChosen = true;
            cell.setLearnState(true);
            break;
          }
        }
      }
    }
    if (!buPredicted) {
      for (Cell cell : cells) {
        cell.setActiveState(true);
      }
    }
    if (!lcChosen && region.getTemporalLearning()) {
      //TODO we need to consider only seq segments when looking for the best segment here in phase 1 - this is for new connections only, can't select future activated segments here
      BestMatchingCellAndSegment bestMatchingCellAndSegment = getBestMatchingCell(Cell.BEFORE);
      Cell bestCell = bestMatchingCellAndSegment.getCell();
      DistalDendriteSegment learningCellBestSegment = bestMatchingCellAndSegment.getSegment();
      bestCell.setLearnState(true);
      // segmentUpdate is added internally to the bestCell's update list.
      DistalDendriteSegment.Update segmentUpdate = bestCell.getSegmentActiveSynapses(learningCellBestSegment,
                                                                                     Cell.BEFORE, true, null);
    }
  }

  /**
   * Phase: 2
   * <p/>
   * WP
   * TEMPORAL POOLING
   * <p/>
   * The second phase calculates the predictive state for each cell.
   * A cell will turn on its predictive state output if one of its
   * segments becomes active, i.e. if enough of its lateral inputs are currently active due to feed-forward input.
   * In this case, the cell queues up the following changes: a) reinforcement of the currently
   * active segment (lines 47-48), and b) reinforcement of a segment that could have predicted this activation,
   * i.e. a segment that has a (potentially weak) match to activity during the previous time step (lines 50-53).
   */
  public void computeCellsPredictiveState() {
    for (Cell cell : cells) {
      for (DistalDendriteSegment segment : cell.getSegments()) {
        if (segment.segmentActive(Cell.NOW, Cell.State.ACTIVE)) {
          //By Kirill - if segment is seq it also should be in learning state to predict
          if (segment.isSequenceSegment() && !segment.segmentActive(Cell.NOW, Cell.State.LEARN)) {
            continue;
          }
          //By Kirill
          /*Cell can't be predicted if in learning state, otherwise it's learning state will case
          adding segments update from phase 1 & 2 in following phase 3 within the same step, but we don't know if the cell will be active in next step
           */
          if (cell.getLearnState(Cell.NOW)) {
            continue;
          }
          cell.setPredictInStepState(segment.predictedInStep());
          if (region.getTemporalLearning()) {
            DistalDendriteSegment.Update activeUpdate = cell.getSegmentActiveSynapses(segment, Cell.NOW, false,
                                                                                      segment.getPredictedBy());
            DistalDendriteSegment.Update previousUpdate = cell.getSegmentActiveSynapses(cell.getBestMatchingSegment(
                    Cell.BEFORE), Cell.BEFORE, true, segment);
          }
        }
      }
    }

  }

  /**
   * Phase: 3
   * <p/>
   * WP
   * TEMPORAL POOLING
   * <p/>
   * The third and last phase actually carries out learning.
   * In this phase segment updates that have been queued up are actually
   * implemented once we get feed-forward input and the cell is chosen as
   * a learning cell (lines 56-57). Otherwise, if the cell ever stops predicting
   * for any reason, we negatively reinforce the segments (lines 58-60).
   */
  public void updateDistalSynapses() {
    for (Cell cell : cells) {
      if (cell.getLearnState(Cell.NOW)) {
        cell.adaptSegments(true);
      } else if (cell.getPredictiveState(Cell.BEFORE) && !cell.getPredictiveState(Cell.NOW)) {
        cell.adaptSegments(false);
      } else if (cell.getPredictInStepState(Cell.NOW) >= cell.getPredictInStepState(
              Cell.BEFORE) && cell.getPredictInStepState(Cell.BEFORE) != Cell.NOT_IN_STEP_PREDICTION) {
        cell.adaptSegmentsForWrongPrediction();

      }
    }
  }


  /**
   * WP
   * <p/>
   * getBestMatchingCell(c)
   * For the given column, return the cell with the best matching segment.
   * If no cell has a matching segment, then return the cell with the fewest number of segments.
   *
   * @param time
   * @return
   */

  private BestMatchingCellAndSegment getBestMatchingCell(int time) {
    List<DistalDendriteSegment> bestMatchingSegmentsFromCells = new ArrayList<DistalDendriteSegment>();
    boolean allSegmentsCreated = true;
    for (Cell cell : cells) {
      if (cell.getSegments().size() == 0) {
        allSegmentsCreated = false;
        break;
      }
    }
    Cell minSegmentListCell = cells.get(0);
    for (Cell cell : cells) {
      //By Kirill
      //Avoid selecting learning cell as best matching to make sure we use next cell in column to help with temporal forking
      if(cell.getLearnState(time)){
        //Shift minCell to next, since we exclude this one
        int nextInx = cell.getCellIndex() + 1;
        if(nextInx < cells.size()){
          minSegmentListCell = cells.get(nextInx);
        } else if(cell.getBestMatchingSegment(time) == null){
          //LOG.warn("Possible repeating pattern, please increase number of cells in column");
        }
        continue;
      }
      if (cell.getSegments().size() < minSegmentListCell.getSegments().size()) {
        minSegmentListCell = cell;
      }
      DistalDendriteSegment bestMatchingSegment = cell.getBestMatchingSegment(time);
      if (bestMatchingSegment != null) {
        bestMatchingSegmentsFromCells.add(bestMatchingSegment);
      }
    }
    DistalDendriteSegment columnBestMatchingSegment = Cell.getBestMatchingSegment(this, bestMatchingSegmentsFromCells,
                                                                                  time);

    return new BestMatchingCellAndSegment(
            columnBestMatchingSegment != null ? columnBestMatchingSegment.getBelongsToCell() : minSegmentListCell,
            columnBestMatchingSegment);
  }

  private static class BestMatchingCellAndSegment {
    private final Cell cell;
    private final DistalDendriteSegment segment;

    private BestMatchingCellAndSegment(Cell bestCell, DistalDendriteSegment bestSegment) {
      this.cell = bestCell;
      this.segment = bestSegment;
    }

    public Cell getCell() {
      return cell;
    }

    public DistalDendriteSegment getSegment() {
      return segment;
    }
  }

  public void nextTimeStep() {
    for (Cell cell : cells) {
      cell.nextTimeStep();
    }
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
      result = 1.0 * activeCount / length;
      return result;
    }

    protected abstract boolean positiveCondition(E state);
  }

  public static class Config {
    private final int amountOfProximalSynapses;
    private final int minOverlap;
    private final int desiredLocalActivity;
    private final double boostRate;

    public Config(int amountOfProximalSynapses, int minOverlap, int desiredLocalActivity,
                  double boostRate) {
      this.amountOfProximalSynapses = amountOfProximalSynapses;
      this.minOverlap = minOverlap;
      this.desiredLocalActivity = desiredLocalActivity;
      this.boostRate = boostRate;
    }


    public int getAmountOfProximalSynapses() {
      return amountOfProximalSynapses;
    }

    public int getMinOverlap() {
      return minOverlap;
    }

    public int getDesiredLocalActivity() {
      return desiredLocalActivity;
    }

    public double getBoostRate() {
      return boostRate;
    }
  }
}
