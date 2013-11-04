package htm.model;

import htm.model.space.BaseSpace;
import htm.model.space.InputSpace;

public class Synapse {
  private double permanence;

  public Synapse(double initPermanence) {
    this.permanence = initPermanence;
  }

  /**
   * synapse is considered connected if its permanence is bigger than
   * connectedPermanence
   *
   * @param connectedPermanence
   * @return
   */
  public boolean isConnected(double connectedPermanence) {
    return this.permanence >= connectedPermanence;
  }


  public double getPermanence() {
    return permanence;
  }

  public void setPermanence(double d) {
    this.permanence = Math.min(Math.max(d, 0), 1);
  }

  public static class ProximalSynapse extends Synapse {
    private final InputSpace.Input connectedSensoryInput;
    private final Column belongsTo;
    private final double distanceToColumn;
    public ProximalSynapse(double initPermanence, InputSpace.Input connectedSensoryInput, Column belongsTo) {
      super(initPermanence);
      this.connectedSensoryInput = connectedSensoryInput;
      this.belongsTo =  belongsTo;
      this.distanceToColumn = BaseSpace.getDistance(belongsTo.getRegion().convertInputPositionToColumnSpace(
              connectedSensoryInput.getPosition()),
                                               belongsTo.getPosition());
    }


    public InputSpace.Input getConnectedSensoryInput() {
      return connectedSensoryInput;
    }

    public double getDistanceToColumn() {
      return distanceToColumn;
    }
  }

  public static class DistalSynapse extends Synapse {
    public DistalSynapse(double initPermanence) {
      super(initPermanence);
    }
  }
}
