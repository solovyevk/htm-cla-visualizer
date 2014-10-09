/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.algorithms.temporal;

import htm.model.Cell;
import htm.model.DistalDendriteSegment;

public class NUPICTemporalPooler extends WhitePaperTemporalPooler {
  public NUPICTemporalPooler(Config cfg) {
    super(cfg);
  }

  @Override public DistalDendriteSegment getActiveSegment(Cell cell, int time, Cell.State state) {
    return super.getActiveSegment(cell, time, state);
  }
}
