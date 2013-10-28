package htm.model;

import java.awt.*;

public class Region {
  private final Dimension dimension;
  private final Column[] columns;

  public Region(Dimension dimension) {
    this.dimension = dimension;
    this.columns = new Column[dimension.width * dimension.height];
    int index = -1;
    for (int y = 0; y < dimension.height; y++) {
      for (int x = 0; x < dimension.width; x++) {
        index++;
        columns[index] = new Column(this, index, new Point(x, y));
      }
    }
  }

  public Region(int xSize, int ySize){
    this(new Dimension(xSize, ySize));
  }

  public Column[] getColumns() {
    return columns;
  }

  public Dimension getDimension() {
    return dimension;
  }
}
