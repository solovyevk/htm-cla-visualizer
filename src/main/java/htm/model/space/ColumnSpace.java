package htm.model.space;

import htm.model.Column;
import htm.model.Region;

import java.awt.*;

public class ColumnSpace extends BaseSpace<Column> {
  public ColumnSpace(int xSize, int ySize) {
    super(xSize, ySize);
  }

  public ColumnSpace(Dimension dimension) {
    super(dimension);
  }

  @Override protected Column createElement(BaseSpace<Column> space, int index, Point position) {
    return new Column((Region)space, index, position);
  }

  public java.util.List<Column> getColumns() {
    return elementList;
  }
}
