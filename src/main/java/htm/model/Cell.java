package htm.model;

import htm.model.fractal.Composite;
import htm.utils.CircularArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class Cell extends Composite<Column, DistalDendriteSegment>{
  private static final Log LOG = LogFactory.getLog(Cell.class);

  /**
   * WP
   * <p/>
   * newSynapseCount
   * The maximum number of synapses added to a segment during learning.
   */

  public static int AMOUNT_OF_SYNAPSES = 30;
  /**
   * cell will keep a buffer of its last TIME_STEPS states
   */
  public static int TIME_STEPS = 6;

  public int getCellIndex() {
    return cellIndex;
  }

  public List<DistalDendriteSegment.Update> getSegmentUpdates() {
    return segmentUpdates;
  }

  public enum State {
    ACTIVE,
    LEARN
  }


  //current and step before
  public static final int BEFORE = 1;
  public static final int NOW = 0;

  //no prediction in up coming steps
  public static final int NOT_IN_STEP_PREDICTION = -1;


  private final int cellIndex;
  /**
   * Boolean vector of Cell's active state in time t-n, ..., t-1, t
   */
  private final CellStateBuffer activeState = new CellStateBuffer();
  /**
   * learnState(c, i, t) A boolean indicating whether cell i in column c is
   * chosen as the cell to learn on.
   */
  private final CellStateBuffer learnState = new CellStateBuffer();

  /**
   * Boolean vector of Cell's predictive state in time t-n, ..., t-1, t
   */
  //private CellStateBuffer predictiveState = new CellStateBuffer();

  private final PredictInStepBuffer predictedInStepState = new PredictInStepBuffer();


  private final List<DistalDendriteSegment.Update> segmentUpdates = new ArrayList<DistalDendriteSegment.Update>();

  public static void updateFromConfig(Config cellCfg) {
    AMOUNT_OF_SYNAPSES = cellCfg.getAmountOfSynapses();
    TIME_STEPS = cellCfg.getTimeSteps();
  }

  public Cell(Column belongsToColumn, int cellIndex) {
    this.owner = belongsToColumn;
    this.cellIndex = cellIndex;
  }

  public List<DistalDendriteSegment> getSegments(){
   return getElements();
  }

  /*
 *Set Learn State in current time Cell.NOW
  */
  public void setLearnState() {
    if(!this.getActiveState(Cell.NOW)){
      LOG.warn("Setting non active cell as learning:" + this);
    }
    this.learnState.setState();
  }

  /**
   * Get Learn state in Time
   *
   * @param time
   */
  public boolean getLearnState(int time) {
    return this.learnState.get(time);
  }

  /**
   * Set active state
   */
  public void setActiveState() {
    this.activeState.setState();
     /*Added by Kirill to track speed of permanence changes for active cells*/
    for (DistalDendriteSegment segment : this.elementList) {
      for (Synapse.DistalSynapse distalSynapse : segment.getElementsList()) {
        distalSynapse.updatePermanenceRangeForActiveCell();
      }
    }
  }



  /**
   * Get Active state in Time
   * <p/>
   * WP
   * <p/>
   * activeState(c, i, t)
   * <p/>
   * A boolean vector with one number per cell.
   * It represents the active state of the column c cell i
   * at time t given the current feed-forward input and the
   * past temporal context.
   * activeState(c, i, t) is the contribution from column c cell i at time t.
   * If true, the cell has current feed-forward input as well as an appropriate temporal context.
   *
   * @param time
   */
  public boolean getActiveState(int time) {
    return this.activeState.get(time);
  }

  /**
   * Set Predictive state
   */
  /*public void setPredictiveState(boolean predictiveState) {
    this.predictiveState.setState(predictiveState);
  }*/

  /**
   * Get Predictive state in Time
   * <p/>
   * WP
   * <p/>
   * predictiveState(c, i, t)
   * A boolean vector with one number per cell.
   * It represents the prediction of the column c cell i at time t,
   * given the bottom-up activity of other columns and the past temporal context.
   * predictiveState(c, i, t) is the contribution of column c cell i at time t.
   * If 1, the cell is predicting feed-forward input in the current temporal context.
   *
   * @param time
   */
  public boolean getPredictiveState(int time) {
    //return this.predictiveState.get(time);
    return getPredictInStepState(time) != NOT_IN_STEP_PREDICTION;
  }

  public void setPredictInStepState(int step) {
    predictedInStepState.setPredictInStep(step);
  }

  public int getPredictInStepState(int time) {
    return predictedInStepState.get(time);
  }



  public boolean deleteSegment(DistalDendriteSegment toDelete) {
    return elementList.remove(toDelete);
  }

  public void deleteAllSegment() {
    elementList.clear();
  }


  @Override public String toString() {
    StringBuilder result = new StringBuilder().append("Column Inx:").append(this.getOwner().getIndex());
    result = result.append("; Cell Inx:").append(this.getCellIndex());
    result = result.append("; Position:").append(this.getOwner().getPosition());
    result = result.append("; Active:").append(this.getActiveState(Cell.NOW));
    result = result.append("; Learn:").append(this.getLearnState(Cell.NOW));
    result = result.append("; Predicted:").append(this.getPredictiveState(Cell.NOW));
    return result.toString();
  }



  public List<Column> getNeighborsAndMyColumn() {
    return this.owner.getOwner().getAllWithinRadius(this.owner.getPosition(),
                                                               this.owner.getOwner().getLearningRadius());

  }




  /*Custom events implementation*/
  private final Collection<SegmentsChangeEventListener> _segmentsChangeEventListeners = new HashSet<SegmentsChangeEventListener>();

  public synchronized void addSegmentsChangeListener(SegmentsChangeEventListener listener) {
    _segmentsChangeEventListeners.add(listener);
  }

  public synchronized void removeSegmentsChangeListener(SegmentsChangeEventListener listener) {
    _segmentsChangeEventListeners.remove(listener);
  }

  public synchronized void fireUpdatesChange() {
    SegmentsChangeEvent event = new SegmentsChangeEvent(this);
    for (SegmentsChangeEventListener segmentsChangeEventListener : _segmentsChangeEventListeners) {
      segmentsChangeEventListener.onUpdatesChange(event);
    }
  }

  public synchronized void fireSegmentsChange() {
    SegmentsChangeEvent event = new SegmentsChangeEvent(this);
    for (SegmentsChangeEventListener segmentsChangeEventListener : _segmentsChangeEventListeners) {
      segmentsChangeEventListener.onSegmentsChange(event);
    }
  }


  public static class SegmentsChangeEvent extends java.util.EventObject {

    public SegmentsChangeEvent(Cell source) {
      super(source);
    }

  }

  public interface SegmentsChangeEventListener {
    public void onSegmentsChange(SegmentsChangeEvent e);

    public void onUpdatesChange(SegmentsChangeEvent e);
  }


  /*
 *Advances this cell to the next time step.
 *The current state of this cell (active, learning, predicting) will be set as the
 *previous state and the current state will be reset to no cell activity by
 *default until it can be determined.
 *Call this function before each temporal cycle
  */
  public void nextTimeStep() {
    this.activeState.add(Cell.NOW, false);
    this.predictedInStepState.add(Cell.NOW, NOT_IN_STEP_PREDICTION);
    this.learnState.add(Cell.NOW, false);
  }

  private static class CellStateBuffer extends CircularArrayList<Boolean> {
    public CellStateBuffer() {
      super(TIME_STEPS);
      for (int i = 0; i < TIME_STEPS; i++) {
        this.add(false);
      }
    }

    /**
     * Set the current state(time NOW)
     *
     */
    void setState() {
      this.set(NOW, true);
    }
  }

  private static class PredictInStepBuffer extends CircularArrayList<Integer> {
    public PredictInStepBuffer() {
      super(TIME_STEPS);
      for (int i = 0; i < TIME_STEPS; i++) {
        this.add(NOT_IN_STEP_PREDICTION);
      }
    }

    void setPredictInStep(int step) {
      this.set(NOW, step);
    }
  }

  public static class Config {
    private final int amountOfSynapses;
    private final int timeSteps;

    public Config(int amountOfSynapses, int timeSteps) {
      this.amountOfSynapses = amountOfSynapses;
      this.timeSteps = timeSteps;
    }


    public int getAmountOfSynapses() {
      return amountOfSynapses;
    }

    public int getTimeSteps() {
      return timeSteps;
    }
  }

}