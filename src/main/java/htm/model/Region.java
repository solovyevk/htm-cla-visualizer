package htm.model;

import htm.model.space.ColumnSpace;
import htm.model.space.InputSpace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;


public class Region extends ColumnSpace {
  private static final Log LOG = LogFactory.getLog(Region.class);
  public Region(int xSize, int ySize) {
    super(xSize, ySize);
  }

  public Region(Dimension dimension) {
    super(dimension);
  }

  public Region(int xSize, int ySize, InputSpace source, double inputRadius) {
    super(xSize, ySize);
    connectToInputSpace(source, inputRadius);
  }

  public Region(int xSize, int ySize, InputSpace source) {
    this(xSize, ySize, source, -1);
  }

  public void connectToInputSpace(InputSpace source, double inputRadius){
    Column[] columns = this.getColumns();
    for (Column column : columns) {
      column.createProximalSegment(source, inputRadius);
    }
  }

}
