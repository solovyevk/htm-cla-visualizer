package htm.model;

import htm.model.space.ColumnSpace;

import java.awt.*;

public class Region extends ColumnSpace {
  public Region(int xSize, int ySize) {
    super(xSize, ySize);
  }

  public Region(Dimension dimension) {
    super(dimension);
  }

  public Column[] getColumns() {
    return elementList.toArray(new Column[elementList.size()]);
  }
}
