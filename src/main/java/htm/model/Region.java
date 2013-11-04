package htm.model;

import htm.model.space.ColumnSpace;
import htm.model.space.InputSpace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;


public class Region extends ColumnSpace {
  private final InputSpace inputSpace;

  private static final Log LOG = LogFactory.getLog(Region.class);


  public Region(int xSize, int ySize, InputSpace source, double inputRadius) {
    super(xSize, ySize);
    this.inputSpace = source;
    connectToInputSpace(inputRadius);
  }

  public Region(int xSize, int ySize, InputSpace source) {
    this(xSize, ySize, source, -1);
  }

  public void connectToInputSpace(double inputRadius) {
    Column[] columns = this.getColumns();
    for (Column column : columns) {
      column.createProximalSegment(inputRadius);
    }
  }

  public Point convertColumnPositionToInputSpace(Point columnPosition) {
    return convertPositionToOtherSpace(columnPosition, this.getDimension(), inputSpace.getDimension());
  }

  public Point convertInputPositionToColumnSpace(Point inputPosition) {
    return convertPositionToOtherSpace(inputPosition, inputSpace.getDimension(), this.getDimension());
  }

  /**
   * WP
   * averageReceptiveFieldSize()
   * The radius of the average connected receptive field size of all the columns.
   * The connected receptive field size of a column includes only the connected synapses (those with permanence values >= connectedPerm).
   * This is used to determine the extent of lateral inhibition between columns.
   */

  public double getAverageReceptiveFieldSize() {
    Column[] columns = this.getColumns();
    double sum = 0;
    for (Column column : columns) {
      java.util.List<Synapse.ProximalSynapse> connectedSynapses = column.getConnectedSynapses();
      double maxDistance = 0;
      for (Synapse.ProximalSynapse connectedSynapse : connectedSynapses) {
        // Determine the distance of the further proximal synapse. This will be considered the size of the receptive field.
        double distanceToInput = connectedSynapse.getDistanceToColumn();
			  maxDistance = Math.max(maxDistance, connectedSynapse.getDistanceToColumn());
      }
      LOG.debug("maxDistance for column:#" + column.getIndex() +" - " + maxDistance);
		// Add the current column's receptive field size to the sum.
		sum += maxDistance;
    }
    return sum/columns.length;
  }


  public InputSpace getInputSpace() {
    return inputSpace;
  }
}

