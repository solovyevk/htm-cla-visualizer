package htm.model;

import htm.model.space.BaseSpace;
import htm.model.space.InputSpace;
import htm.utils.CircularArrayList;
import htm.utils.MathUtils;

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

    public static void updateFromConfig(Config synapseCfg) {
      ProximalSynapse.CONNECTED_PERMANENCE = synapseCfg.getConnectedPerm();
      ProximalSynapse.PERMANENCE_INCREASE = synapseCfg.getPermanenceInc();
      ProximalSynapse.PERMANENCE_DECREASE = synapseCfg.getPermanenceDec();
    }

    private final InputSpace.Input connectedSensoryInput;
    private final Column belongsTo;
    private final double distanceToColumn;

    public ProximalSynapse(double initPermanence, InputSpace.Input connectedSensoryInput, Column belongsTo) {
      super(initPermanence);
      this.connectedSensoryInput = connectedSensoryInput;
      this.belongsTo = belongsTo;
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

    public Column getBelongsTo() {
      return belongsTo;
    }
  }

  public static class DistalSynapse extends Synapse {

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

    private final Cell fromCell;
    private DistalDendriteSegment segment;


    public static void updateFromConfig(Config synapseCfg) {
      DistalSynapse.CONNECTED_PERMANENCE = synapseCfg.getConnectedPerm();
      DistalSynapse.PERMANENCE_INCREASE = synapseCfg.getPermanenceInc();
      DistalSynapse.PERMANENCE_DECREASE = synapseCfg.getPermanenceDec();
    }

    public DistalSynapse(Cell fromCell) {
      //NOT sure how to deal with initial permanence, do following for now
      this(CONNECTED_PERMANENCE - 3 * PERMANENCE_INCREASE, fromCell);
    }

    public DistalSynapse(double initPermanence, Cell fromCell) {
      super(initPermanence);
      this.fromCell = fromCell;
    }

    public Cell getFromCell() {
      return fromCell;
    }

    public DistalDendriteSegment getSegment() {
      return segment;
    }

    public void setSegment(DistalDendriteSegment segment) {
      this.segment = segment;
    }

    /*Added by Kirill to track speed of permanence changes for active cells*/

    public static final int PERMANENCE_RANGE_BUFFER_SIZE = 20;
    private PermanenceBufferedState<Double> permanenceRangeForActiveCell = new PermanenceBufferedState<Double>();

    public void updatePermanenceRangeForActiveCell() {
      permanenceRangeForActiveCell.addState(this.getPermanence());
    }

    public double getPermanenceRangeChangeForActive(){
      double currentPermanence = getPermanence();
      if(currentPermanence == 1.0 || permanenceRangeForActiveCell.size() < PERMANENCE_RANGE_BUFFER_SIZE){
        return 1.0;
      } else {
        Double[] permanenceRange = (Double[])permanenceRangeForActiveCell.toArray(new Double[permanenceRangeForActiveCell.size()]);
        return MathUtils.findMax(permanenceRange) - MathUtils.findMin(permanenceRange);
      }
    }

    private static class PermanenceBufferedState<Double> extends CircularArrayList<Double> {
      public PermanenceBufferedState() {
        super(PERMANENCE_RANGE_BUFFER_SIZE);
      }

      void addState(Double value) {
        this.add(0, value);
      }

      public Double getLast() {
        return this.get(0);
      }
    }
  }


  public static class Config {
    private final double connectedPerm;
    private final double permanenceInc;
    private final double permanenceDec;


    public Config(double connectedPerm, double permanenceInc, double permanenceDec) {
      this.connectedPerm = connectedPerm;
      this.permanenceInc = permanenceInc;
      this.permanenceDec = permanenceDec;
    }

    public double getConnectedPerm() {
      return connectedPerm;
    }

    public double getPermanenceInc() {
      return permanenceInc;
    }

    public double getPermanenceDec() {
      return permanenceDec;
    }
  }
}
