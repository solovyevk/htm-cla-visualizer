/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model;

import htm.model.fractal.Composite;
import htm.utils.CollectionUtils;

import java.util.List;

public class DistalDendriteSegment extends Composite<Cell, Synapse.DistalSynapse> {

  protected final DistalDendriteSegment predictedBy;


  //We need to check if synapse connected to this cell is already exist before adding new one
  @Override public boolean addElement(Synapse.DistalSynapse distalSynapse) {
    Cell newSynapseCell = distalSynapse.getFromCell();
    for (Synapse.DistalSynapse existingSynapse : elementList) {
      if (existingSynapse.getFromCell() == newSynapseCell) {
        return false;
      }
    }
    distalSynapse.setSegment(this);
    return super.addElement(distalSynapse);
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder().append("Synapses Number:").append(this.elementList.size());
    result = result.append("; sequence Segment:").append(this.isSequenceSegment());
    result.append("; Belongs to Cell:").append(this.owner);
    return result.toString();
  }


  public DistalDendriteSegment(Cell belongsToCell, DistalDendriteSegment predictedBy) {
    this.owner = belongsToCell;
    this.predictedBy = predictedBy;
    attachToCell();
  }

  protected void attachToCell() {
    owner.addElement(this);
  }

  public boolean isSequenceSegment() {
    return predictedBy == null;
  }

  public int size(){return elementList.size();}

  public boolean contains(Synapse.DistalSynapse synapse){
   return elementList.contains(synapse);
  }

  /**
   * WP
   * segmentActive(s, t, state)
   * <p/>
   * This routine returns true if the number of connected synapses on segment s that are active due to the given
   * state at time t is greater than activationThreshold. The parameter state can be activeState, or learnState.
   */
  public boolean segmentActive(int time, Cell.State state, int activationThreshold) {
    return getConnectedWithStateCell(time, state).size() > activationThreshold;
  }

  public List<Synapse.DistalSynapse> getConnectedWithStateCell(int time, Cell.State state) {
    return CollectionUtils.filter(this.elementList, new ConnectedCellStateByTimePredicate(time, state));
  }

  public List<Synapse.DistalSynapse> getActiveCellSynapses(int time) {
    return CollectionUtils.filter(this.elementList, new ActiveCellByTimePredicate(time));
  }



  public int predictedInStep() {
    int result = 1;
    DistalDendriteSegment predictedBySegment = this.predictedBy;
    while (predictedBySegment != null) {
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
      //return synapse.getFromCell().getActiveState(time);
      //By Kirill
      return synapse.getFromCell().getActiveState(time) &&  synapse.getFromCell().getLearnState(time);
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

    public Update(Cell belongsToCell, DistalDendriteSegment target, int time, DistalDendriteSegment predictedBy) {
      super(belongsToCell, predictedBy);
      this.target = target;
      this.time = time;
    }

    @Override
    public int predictedInStep() {
      //Segment for this update hasn't been created yet
      if (target == null) {
        return predictedBy == null ? 1 : predictedBy.predictedInStep() + 1;
      } else {
        return target.predictedInStep();
      }
    }

    @Override public boolean isSequenceSegment() {
      if (target == null) {
        return predictedBy == null;
      } else {
        return target.isSequenceSegment();
      }
    }

    @Override
    protected void attachToCell() {
      this.owner.getSegmentUpdates().add(this);
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
      StringBuilder result = new StringBuilder().append(" New Segment:").append(this.isNewSegment());
      result = result.append("; Time:").append(this.time);
      result.append(super.toString());
      return result.toString();
    }

  }
}
