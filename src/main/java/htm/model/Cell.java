package htm.model;

import htm.utils.CircularArrayList;
import htm.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Cell {

  public int getCellIndex() {
    return cellIndex;
  }

  public Column getBelongsToColumn() {
    return belongsToColumn;
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

  /**
   * cell will keep a buffer of its last TIME_STEPS states
   */
 public static int TIME_STEPS = 6;

  private final Column belongsToColumn;
  private final int cellIndex;
  /**
   * Boolean vector of Cell's active state in time t-n, ..., t-1, t
   */
  private CellStateBuffer activeState = new CellStateBuffer();

  /**
   * Boolean vector of Cell's predictive state in time t-n, ..., t-1, t
   */
  private CellStateBuffer predictiveState = new CellStateBuffer();
  /**
   * learnState(c, i, t) A boolean indicating whether cell i in column c is
   * chosen as the cell to learn on.
   */
  private CellStateBuffer learnState = new CellStateBuffer();

  protected final List<DistalDendriteSegment> segments = new ArrayList<DistalDendriteSegment>();

  private final List<DistalDendriteSegment.Update> segmentUpdates = new ArrayList<DistalDendriteSegment.Update>();

  public Cell(Column belongsToColumn, int cellIndex) {
    this.belongsToColumn = belongsToColumn;
    this.cellIndex = cellIndex;
    //TODO REMOVE
    /*    for (int i = 0; i < 10; i++) {
          new DistalDendriteSegment(this);
        }  */
  }

  /*
 *Set Learn State in current time Cell.NOW
  */
  public void setLearnState(boolean learnState) {
    this.learnState.setState(learnState);
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
  public void setActiveState(boolean activeState) {
    this.activeState.setState(activeState);
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
  public void setPredictiveState(boolean predictiveState) {
    this.predictiveState.setState(predictiveState);
  }

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
    return this.predictiveState.get(time);
  }

  /**
   * WP
   * <p/>
   * getActiveSegment(c, i, t, state)
   * <p/>
   * For the given column c cell i, return a segment index such that segmentActive(s,t, state) is true.
   * If multiple segments are active, sequence segments are given preference.
   * Otherwise, segments with most activity are given preference.
   */

  public DistalDendriteSegment getActiveSegment(final int time, final State state) {
    List<DistalDendriteSegment> activeSegments = CollectionUtils.filter(this.segments,
                                                                        new CollectionUtils.Predicate<DistalDendriteSegment>() {
                                                                          @Override
                                                                          public boolean apply(
                                                                                  DistalDendriteSegment segment) {
                                                                            return segment.segmentActive(time, state);
                                                                          }
                                                                        });
    Collections.sort(activeSegments, new Comparator<DistalDendriteSegment>() {
      @Override
      public int compare(DistalDendriteSegment segment, DistalDendriteSegment segmentToCompare) {
        int amountActiveCells = segment.getConnectedWithStateCell(time, state).size();
        int amountActiveCellsToCompare = segmentToCompare.getConnectedWithStateCell(time, state).size();
        if (segment.isSequenceSegment() == segmentToCompare.isSequenceSegment()
            && amountActiveCells == amountActiveCellsToCompare) {
          return 0;
        } else if ((segment.isSequenceSegment() && !segmentToCompare.isSequenceSegment())
                   || (segment.isSequenceSegment() == segmentToCompare.isSequenceSegment()
                       && amountActiveCells > amountActiveCellsToCompare)) {
          return 1;
        } else {
          return -1;
        }
      }
    });
    return activeSegments.size() > 0 ? activeSegments.get(activeSegments.size() - 1) : null;
  }

  /**
   * WP
   * <p/>
   * getBestMatchingSegment(c, i, t)
   * <p/>
   * For the given column c cell i at time t, find the segment with the largest number of active synapses.
   * This routine is aggressive in finding the best match. The permanence value of synapses is allowed to be
   * below connectedPerm. The number of active synapses is allowed to be below activationThreshold,
   * but must be above minThreshold. The routine returns the segment index. If no segments are found, then an index of -1 is returned.
   *
   * @param time
   * @return
   */
  public DistalDendriteSegment getBestMatchingSegment(final int time) {
    return getBestMatchingSegment(new ArrayList<DistalDendriteSegment>(this.getSegments()), time);
  }

  public static DistalDendriteSegment getBestMatchingSegment(List<DistalDendriteSegment> segmentList, final int time) {
    Collections.sort(segmentList, new Comparator<DistalDendriteSegment>() {
      @Override
      public int compare(DistalDendriteSegment segment, DistalDendriteSegment segmentToCompare) {
        int amountActiveCells = segment.getActiveCellSynapses(time).size();
        int amountActiveCellsToCompare = segmentToCompare.getActiveCellSynapses(time).size();
        if (amountActiveCells == amountActiveCellsToCompare) {
          return 0;
        } else if (amountActiveCells > amountActiveCellsToCompare) {
          return 1;
        } else {
          return -1;
        }
      }
    });
    return segmentList.size() > 0 && segmentList.get(segmentList.size() - 1).getActiveCellSynapses(
            time).size() > DistalDendriteSegment.MIN_THRESHOLD ? segmentList.get(segmentList.size() - 1) : null;
  }

  public List<DistalDendriteSegment> getSegments() {
    return Collections.unmodifiableList(segments);
  }

  /**
   * If the segment is NULL, then a new segment is to be added, otherwise
   * the specified segment is updated.  If the segment exists, find all active
   * synapses for the segment (either at t or t-1)
   * and mark them as needing to be updated.  If newSynapses is true, then
   * Region.newSynapseCount - len(activeSynapses) new synapses are added to the
   * segment to be updated.  The (new) synapses are randomly chosen from the set
   * of current learning cells (within Region.predictionRadius if set).
   * These segment updates are only applied when the applySegmentUpdates
   * method is later called on this Cell.
   * <p/>
   * WP
   * <p/>
   * getSegmentActiveSynapses(c, i, t, s, newSynapses= false)
   * <p/>
   * Return a segmentUpdate data structure containing a list of proposed changes to segment s.
   * Let activeSynapses be the list of active synapses where the originating cells have their
   * activeState output = 1 at time step t. (This list is empty if s = -1 since the segment doesn't exist.)
   * newSynapses is an optional argument that defaults to false. If newSynapses is true,
   * then newSynapseCount - count(activeSynapses) synapses are added to activeSynapses.
   * These synapses are randomly chosen from the set of cells that have learnState output = 1 at time step t.
   */
  public DistalDendriteSegment.Update getSegmentActiveSynapses(DistalDendriteSegment segment, int time,
                                                               boolean newSynapses) {
    DistalDendriteSegment.Update result = new DistalDendriteSegment.Update(this, segment);
    if (segment != null) {
      result.addAll(segment.getActiveCellSynapses(time));
    }
    int numberOfNewSynapsesToAdd = DistalDendriteSegment.NEW_SYNAPSE_COUNT - result.size();
    if (newSynapses && numberOfNewSynapsesToAdd > 0) {
      List<Column> neighbors = this.belongsToColumn.getRegion().getAllWithinRadius(this.belongsToColumn.getPosition(),
                                                                                   this.belongsToColumn.getRegion().getLearningRadius());
      List<Cell> cellWithLearnStateList = new ArrayList<Cell>();
      for (Column neighborColumn : neighbors) {
        List<Cell> cellList = neighborColumn.getCells();
        for (Cell cell : cellList) {
           /*NOTE: There is no indication in the Numenta pseudocode that a cell shouldn't be able to have a
           *distal synapse from another cell in the same column. Therefore the below check is commented out.
           * Skip cells in our own col (don't connect to ourself)
           * */
          if (cell.belongsToColumn == this.belongsToColumn) {
            continue;
          }
          if (cell.getLearnState(time)) {
            cellWithLearnStateList.add(cell);
          }
        }
      }
      Collections.shuffle(cellWithLearnStateList);
      numberOfNewSynapsesToAdd = cellWithLearnStateList.size() < numberOfNewSynapsesToAdd ? cellWithLearnStateList.size() : numberOfNewSynapsesToAdd;
      for (int i = 0; i < numberOfNewSynapsesToAdd; i++) {
        Cell cellWithLearnState = cellWithLearnStateList.get(i);
        result.add(new Synapse.DistalSynapse(cellWithLearnState));
      }
    }
    return result;
  }

  /**
   * WP
   * <p/>
   * adaptSegments(segmentList, positiveReinforcement)
   * <p/>
   * This function iterates through a list of segmentUpdate's and reinforces each segment.
   * For each segmentUpdate element, the following changes are performed.
   * If positiveReinforcement is true then synapses on the active list get
   * their permanence counts incremented by permanenceInc. All other synapses
   * get their permanence counts decremented by permanenceDec. If positiveReinforcement
   * is false, then synapses on the active list get their permanence counts decremented by permanenceDec.
   * After this step, any synapses in segmentUpdate that do yet exist get added with a permanence count of initialPerm.
   */
  public void adaptSegments(boolean positiveReinforcement) {
    for (DistalDendriteSegment.Update segmentUpdate : segmentUpdates) {
      DistalDendriteSegment segment;
      if (segmentUpdate.isNewSegment()) {
        segment = new DistalDendriteSegment(this);
      } else {
        segment = segmentUpdate.getTarget();
      }
      segment.setSequenceSegment(segmentUpdate.isSequenceSegment());

      for (Synapse.DistalSynapse distalSynapse : segment) {
        if (positiveReinforcement) {
          if (segmentUpdate.contains(distalSynapse)) {
            distalSynapse.setPermanence(distalSynapse.getPermanence() + Synapse.DistalSynapse.PERMANENCE_INCREASE);
          } else {
            distalSynapse.setPermanence(distalSynapse.getPermanence() - Synapse.DistalSynapse.PERMANENCE_DECREASE);
          }
        } else {
          if (segmentUpdate.contains(distalSynapse)) {
            distalSynapse.setPermanence(distalSynapse.getPermanence() - Synapse.DistalSynapse.PERMANENCE_DECREASE);
          }
        }
      }

      for (Synapse.DistalSynapse distalSynapse : segmentUpdate) {
        if (!segment.contains(distalSynapse)) {
          segment.add(distalSynapse);
        }
      }
      //DELETE processed segmentUpdate
      //this.segmentUpdates.remove(segmentUpdate);
    }
    //Clear segmentUpdates after adaption;
    this.segmentUpdates.clear();
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
    this.predictiveState.add(Cell.NOW, false);
    this.learnState.add(Cell.NOW, false);
    //TODO CHECK
    /* Need to reset sequenceSegment flag, since it is only make sense in current time step
     and all predictions  has been done by now*/
    /*for (DendriteSegment segment : segments) {
      segment.setSequenceSegment(false);
    }*/
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
     * @param state
     */
    void setState(boolean state) {
      this.set(NOW, state);
    }
  }
}
