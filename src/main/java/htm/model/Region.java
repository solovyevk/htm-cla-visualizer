/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model;

import java.awt.*;

public class Region {
  private final Point dimension;
  private final Column[] columns;

  public Region(Point dimension) {
    this.dimension = dimension;
    this.columns = new Column[dimension.x * dimension.y];
    int index = -1;
    for (int y = 0; y < dimension.y; y++) {
      for (int x = 0; x < dimension.x; x++) {
        index++;
        columns[index] = new Column(index);
      }
    }
  }

  public Column[] getColumns() {
    return columns;
  }
}
