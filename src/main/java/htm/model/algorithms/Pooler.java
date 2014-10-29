/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.algorithms;

import htm.model.Layer;
import htm.model.algorithms.spatial.SpatialPooler;
import htm.model.algorithms.temporal.TemporalPooler;

public abstract class Pooler {
  protected Layer layer;
  protected boolean learningMode = true;

  public boolean isLearningMode() {
    return learningMode;
  }

  public void setLearningMode(boolean learningMode) {
    this.learningMode = learningMode;
  }

  public Pooler setLayer(Layer layer) {
    this.layer = layer;
    if(TemporalPooler.class.isAssignableFrom(this.getClass())){
      layer.setTemporalPooler((TemporalPooler)this);
    } else {
      layer.setSpatialPooler((SpatialPooler)this);
    }
    return this;
  }

  public abstract void execute();
}
