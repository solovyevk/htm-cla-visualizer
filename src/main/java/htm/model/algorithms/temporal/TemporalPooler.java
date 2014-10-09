/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.algorithms.temporal;

import htm.model.Layer;

public abstract class TemporalPooler {
  /**
   * WP
   * <p/>
   * newSynapseCount
   * The maximum number of synapses added to a segment during learning.
   */

  protected int newSynapseCount;
  /**
   * WP
   * activationThreshold
   * <p/>
   * Activation threshold for a segment. If the number of active connected
   * synapses in a segment is greater than activationThreshold, the segment is said to be active.
   */
  protected int activationThreshold;
  /**
   * WP
   * minThreshold Minimum segment activity for learning.
   */
  protected int minThreshold;

  protected TemporalPooler(Config cfg) {
    this.newSynapseCount = cfg.getNewSynapseCount();
    this.activationThreshold = cfg.getActivationThreshold();
    this.minThreshold = cfg.getMinThreshold();
  }

  protected Layer layer;
  protected boolean learningMode = true;

  public boolean isLearningMode() {
    return learningMode;
  }

  public void setLearningMode(boolean learningMode) {
    this.learningMode = learningMode;
  }

  public TemporalPooler setLayer(Layer layer) {
    this.layer = layer;
    layer.setTemporalPooler(this);
    return this;
  }

  public abstract void execute();

  public int getNewSynapseCount() {
    return newSynapseCount;
  }


  public int getActivationThreshold() {
    return activationThreshold;
  }


  public int getMinThreshold() {
    return minThreshold;
  }

  public static class Config {
    private final int newSynapseCount;
    private final int activationThreshold;
    private final int minThreshold;

    public Config(int newSynapseCount, int activationThreshold, int minThreshold) {
      this.newSynapseCount = newSynapseCount;
      this.activationThreshold = activationThreshold;
      this.minThreshold = minThreshold;
    }

    public int getNewSynapseCount() {
      return newSynapseCount;
    }

    public int getActivationThreshold() {
      return activationThreshold;
    }

    public int getMinThreshold() {
      return minThreshold;
    }

  }
}
