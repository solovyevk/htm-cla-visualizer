package htm.model.space;

import htm.model.Column;
import htm.model.Layer;

import java.awt.*;

public class ColumnSpace<P> extends BaseSpace<P, Column> {
  public ColumnSpace(int xSize, int ySize) {
    super(xSize, ySize);
  }

  public ColumnSpace(Dimension dimension) {
    super(dimension);
  }

  @Override protected Column createElement(BaseSpace<P,Column> space, int index, Point position) {
    return new Column((Layer)space, index, position);
  }

  public java.util.List<Column> getColumns() {
    return this.getElements();
  }
}
