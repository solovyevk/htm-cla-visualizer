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

  protected final Cell belongsToCell;
  private final DistalDendriteSegment  predictedBy;


  //We need to check if synapse connected to this cell is already exist before adding new one
  @Override public boolean add(Synapse.DistalSynapse distalSynapse) {
    Cell newSynapseCell = distalSynapse.getFromCell();
    for (Synapse.DistalSynapse existingSynapse : this) {
      if (existingSynapse.getFromCell() == newSynapseCell) {
        return false;
      }
    }
    distalSynapse.setSegment(this);
    return super.add(distalSynapse);
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder().append("Synapses Number:").append(this.size());
    result = result.append("sequence Segment:").append(this.isSequenceSegment());
    result.append("Belongs to Cell:").append(this.belongsToCell);
    return result.toString();
  }


  public DistalDendriteSegment(Cell belongsToCell, DistalDendriteSegment predictedBy) {
    super(Cell.AMOUNT_OF_SYNAPSES);
    this.belongsToCell = belongsToCell;
    this.predictedBy = predictedBy;
    attachToCell();
  }

  protected void attachToCell() {
    belongsToCell.segments.add(this);
  }

  public boolean isSequenceSegment() {
    return predictedBy == null;
  }

  /**
   * WP
   * segmentActive(s, t, state)
   * <p/>
   * This routine returns true if the number of connected synapses on segment s that are active due to the given
   * state at time t is greater than activationThreshold. The parameter state can be activeState, or learnState.
   */
  public boolean segmentActive(int time, Cell.State state) {
    List<Synapse.DistalSynapse> res = getConnectedWithStateCell(time, state);
    return getConnectedWithStateCell(time, state).size() > Cell.ACTIVATION_THRESHOLD;
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



  public int predictedInStep() {
    int result = 1;
    DistalDendriteSegment predictedBySegment = this.predictedBy;
    while(predictedBySegment != null){
      result = result + 1;
      predictedBySegment = predictedBySegment.getPredictedBy();
    }
    return result;
  }

  public DistalDendriteSegment getPredictedBy() {
    return predictedBy;
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
          return result && synapse.getFromCell().getLearnState(time);
      }
      throw new RuntimeException("We shouldn't get here");
    }
  }

  public static class Update extends DistalDendriteSegment {
    private final DistalDendriteSegment target;
    private final DistalDendriteSegment predictedBy;
    private final int time;

    public Update(Cell belongsToCell, DistalDendriteSegment target, int time, DistalDendriteSegment predictedBy) {
      super(belongsToCell, predictedBy);
      this.target = target;
      this.time = time;
      this.predictedBy = predictedBy;
    }

    @Override protected void attachToCell() {
      this.belongsToCell.getSegmentUpdates().add(this);
    }

    public boolean isNewSegment() {
      return target == null;
    }

    public DistalDendriteSegment getTarget() {
      return target;
    }

    public int getTime() {
      return time;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder().append("New Segment:").append(this.isNewSegment());
      result = result.append("Time:").append(this.time);
      result.append(super.toString());
      return result.toString();
    }

  }
}
