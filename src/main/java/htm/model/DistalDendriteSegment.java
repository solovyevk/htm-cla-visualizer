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
import java.util.Stack;

public class DistalDendriteSegment extends ArrayList<Synapse.DistalSynapse> {

  protected final Cell belongsToCell;
  private boolean sequenceSegment;
  private Stack<DistalDendriteSegment> predictedBySegments;
  //private DistalDendriteSegment nextPredictionSegment;


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
    result = result.append("sequence Segment:").append(this.sequenceSegment);
    result.append("Belongs to Cell:").append(this.belongsToCell);
    return result.toString();
  }


  public DistalDendriteSegment(Cell belongsToCell) {
    super(Cell.AMOUNT_OF_SYNAPSES);
    this.belongsToCell = belongsToCell;
    attachToCell();
  }

  protected void attachToCell() {
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

 /* public void setPredictedBySegment(DistalDendriteSegment nextPredictionSegment) {
    if (sequenceSegment && nextPredictionSegment != null) {
      throw new RuntimeException("Can not set next prediction segment for sequence segment!");
    }
    this.predictedBySegments.push(nextPredictionSegment);
  } */

  public void linkPredictedBySegment(DistalDendriteSegment segment) {
    if (sequenceSegment) {
      throw new RuntimeException("Can not set next prediction segment for sequence segment!");
    }
    predictedBySegments = new Stack<DistalDendriteSegment>();
    if (!segment.isSequenceSegment()) {
      for (DistalDendriteSegment previous : segment.predictedBySegments) {
        this.predictedBySegments.push(previous);
      }
    }
    this.predictedBySegments.push(segment);
  }

  public boolean isLinkedWithPredictedBySegment(){
    return predictedBySegments  != null;
  }

  public DistalDendriteSegment getPredictedBySegment() {
    return this.predictedBySegments.peek();
  }

  public int predictedInStep() {
    return predictedBySegments != null ? predictedBySegments.size() : 0;
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
    private final int time;

    public Update(Cell belongsToCell, DistalDendriteSegment target, int time) {
      super(belongsToCell);
      this.target = target;
      this.time = time;
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
