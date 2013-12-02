/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model;

import htm.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class DistalDendriteSegment extends ArrayList<Synapse.DistalSynapse> {

  private final Cell belongsToCell;
  private boolean sequenceSegment;


  /**
   * WP
   * <p/>
   * newSynapseCount
   * The maximum number of synapses added to a segment during learning.
   */
  public static int NEW_SYNAPSE_COUNT = 5;

  /**
   * WP
   * activationThreshold
   * <p/>
   * Activation threshold for a segment. If the number of active connected
   * synapses in a segment is greater than activationThreshold, the segment is said to be active.
   */
  public static int ACTIVATION_THRESHOLD = 2;

  /**
   * WP
   * minThreshold Minimum segment activity for learning.
   */
  public static int MIN_THRESHOLD = 1;

  public static int AMOUNT_OF_SYNAPSES = 30;


  public DistalDendriteSegment(Cell belongsToCell) {
    super(AMOUNT_OF_SYNAPSES);
    this.belongsToCell = belongsToCell;
    belongsToCell.segments.add(this);
  }


  public boolean isSequenceSegment() {
    return sequenceSegment;
  }

  public void setSequenceSegment(boolean sequenceSegment) {
    this.sequenceSegment = sequenceSegment;
  }

  /**
   * WP
   * segmentActive(s, t, state)
   * <p/>
   * This routine returns true if the number of connected synapses on segment s that are active due to the given
   * state at time t is greater than activationThreshold. The parameter state can be activeState, or learnState.
   */
  public boolean segmentActive(int time, Cell.State state) {
    return getConnectedWithStateCell(time, state).size() > ACTIVATION_THRESHOLD;
  }

  public List<Synapse.DistalSynapse> getConnectedWithStateCell(int time, Cell.State state) {
    return CollectionUtils.filter(this, new ConnectedCellStateByTimePredicate(time, state));
  }

  public List<Synapse.DistalSynapse> getActiveCellSynapses(int time) {
    return CollectionUtils.filter(this, new ActiveCellByTimePredicate(time));
  }

  public Cell getBelongsToCell() {
    return belongsToCell;
  }

  private static class ActiveCellByTimePredicate implements CollectionUtils.Predicate<Synapse.DistalSynapse> {
    private final int time;

    private ActiveCellByTimePredicate(int time) {
      this.time = time;
    }

    @Override
    public boolean apply(Synapse.DistalSynapse synapse) {
      return synapse.getFromCell().getActiveState(time);
    }
  }


  private static class ConnectedCellStateByTimePredicate implements CollectionUtils.Predicate<Synapse.DistalSynapse> {
    private final int time;
    private final Cell.State cellState;

    private ConnectedCellStateByTimePredicate(int time, Cell.State cellState) {
      this.time = time;
      this.cellState = cellState;
    }

    @Override
    public boolean apply(Synapse.DistalSynapse synapse) {
      boolean result = synapse.isConnected(
              Synapse.DistalSynapse.CONNECTED_PERMANENCE);
      switch (cellState) {
        case ACTIVE:
          return result && synapse.getFromCell().getActiveState(time);
        case LEARN:
          return result && synapse.getFromCell().getPredictiveState(time);
      }
      throw new RuntimeException("We shouldn't get here");
    }
  }

  public static class Update extends DistalDendriteSegment {
    private final DistalDendriteSegment target;

    public Update(Cell belongsToCell, DistalDendriteSegment target) {
      super(belongsToCell);
      this.target = target;
    }

    public boolean isNewSegment() {
      return target == null;
    }

    public DistalDendriteSegment getTarget(){
      return target;
    }
  }
}
