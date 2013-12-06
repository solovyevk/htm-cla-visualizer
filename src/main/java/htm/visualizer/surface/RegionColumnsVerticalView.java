/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.surface;

import htm.model.Cell;
import htm.model.Column;
import htm.model.Region;

public class RegionColumnsVerticalView extends CellSurface{
  public RegionColumnsVerticalView(Region region) {
    super(region.getActiveColumns().size(), Column.CELLS_PER_COLUMN, region);
  }

  @Override
  public Cell getCell(int index) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
