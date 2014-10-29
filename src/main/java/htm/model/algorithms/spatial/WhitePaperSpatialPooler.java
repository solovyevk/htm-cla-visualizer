/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.algorithms.spatial;

import htm.model.Column;
import htm.model.Synapse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WhitePaperSpatialPooler extends SpatialPooler {

  private static final Log LOG = LogFactory.getLog(WhitePaperSpatialPooler.class);


  private static final Comparator<Column> OVERLAP_COMPARATOR = new Comparator<Column>() {
    @Override public int compare(Column column1, Column column2) {
      Double overlap1 = column1.getOverlap(), overlap2 = column2.getOverlap();
      return overlap2.compareTo(overlap1);
    }
  };

  public WhitePaperSpatialPooler(Config cfg) {
    super(cfg);
  }

  /**
   * Performs spatial pooling for the current input in this Region.
   * The result will be a subset of Columns being set as active as well
   * as (proximal) synapses in all Columns having updated permanences and boosts, and
   * the Region will update inhibitionRadius.
   * <p/>
   * WP
   * Phase 1:
   * Compute the overlap with the current input for each column. Given an input
   * vector, the first phase calculates the overlap of each column with that
   * vector. The overlap for each column is simply the number of connected
   * synapses with active inputs, multiplied by its boost. If this value is
   * below minOverlap, we set the overlap score to zero.
   * <p/>
   * Phase 2:
   * Compute the winning columns after inhibition. The second phase calculates
   * which columns remain as winners after the inhibition step.
   * desiredLocalActivity is a parameter that controls the number of columns
   * that end up winning. For example, if desiredLocalActivity is 10, a column
   * will be a winner if its overlap score is greater than the score of the
   * 10'th highest column within its inhibition radius.
   * <p/>
   * Phase 3:
   * Update synapse permanence and internal variables.The third phase performs
   * learning; it updates the permanence values of all synapses as necessary,
   * as well as the boost and inhibition radius. The main learning rule is
   * implemented in lines 20-26. For winning columns, if a synapse is active,
   * its permanence value is incremented, otherwise it is decremented. Permanence
   * values are constrained to be between 0 and 1.
   * Lines 28-36 implement boosting. There are two separate boosting mechanisms
   * in place to help a column learn connections. If a column does not win often
   * enough (as measured by activeDutyCycle), its overall boost value is
   * increased (line 30-32). Alternatively, if a column's connected synapses do
   * not overlap well with any inputs often enough (as measured by
   * overlapDutyCycle), its permanence values are boosted (line 34-36).
   */

  @Override
  public void execute() {
    double inhibitionRadius = layer.getAverageReceptiveFieldSize();
    if (layer.isSkipSpatial()) {
      for (Column currentColumn : layer.getElementsList()) {
        currentColumn.setActive(layer.getInputSpace().getInputValue(currentColumn.getIndex()));
      }
    } else {
      phaseOne();
      phaseThree(phaseTwo(inhibitionRadius), inhibitionRadius);
    }
  }

  public void phaseOne() {
    //Phase 1: Compute the overlap
    for (Column column : layer.getElementsList()) {
      computeOverlapForColumn(column);
    }
  }

  public List<Column> phaseTwo(double inhibitionRadius) {
    //Phase 2:Compute the winning columns after inhibition
    List<Column> activeColumns = new ArrayList<Column>();
    for (Column column : layer.getElementsList()) {
      if (computeActiveDoInhibitionForColumn(column, inhibitionRadius)) {
        activeColumns.add(column);
      }
    }
    return activeColumns;
  }


  public void phaseThree(List<Column> activeColumns, double inhibitionRadius) {
    // Phase 3: Update synapse permanence and internal variables
    if (isLearningMode()) {
      for (Column activeColumn : activeColumns) {
        learnSpatialForActiveForColumn(activeColumn);
      }
      for (Column column : layer.getElementsList()) {
        boostWeakForColumn(column, inhibitionRadius);
      }
    }
  }

  /**
   * WP
   * SPATIAL POOLING
   * <p/>
   * Phase 1: Overlap
   * Given an input vector, the first phase calculates the overlap of each column with that vector.
   * The overlap for each column is simply the number of connected synapses with active inputs,
   * multiplied by its boost. If this value is below minOverlap, we set the overlap score to zero.
   *
   * @return
   */
  public double computeOverlapForColumn(Column currentColumn) {
    double currentOverLap = currentColumn.getActiveConnectedSynapses().size();
    if (currentOverLap < this.getMinimalOverlap()) {
      currentOverLap = 0;
    } else {
      currentOverLap = currentOverLap * currentColumn.getBoost();
    }
    currentColumn.updateOverlap(currentOverLap);
    return currentOverLap;
  }

  /**
   * WP
   * SPATIAL POOLING
   * <p/>
   * Phase 2: Inhibition
   * The second phase calculates which columns remain as winners after the inhibition step.
   * desiredLocalActivity is a parameter that controls the number of columns that end up winning.
   * For example, if desiredLocalActivity is 10, a column will be a winner if its overlap score is
   * greater than the score of the 10'th highest column within its inhibition radius.
   *
   * @param inhibitionRadius
   * @return
   */

  public boolean computeActiveDoInhibitionForColumn(Column currentColumn, double inhibitionRadius) {
    double minLocalActivity = kthScore(currentColumn.getNeighbors(inhibitionRadius), this.getDesiredLocalActivity());
    currentColumn.setActive(currentColumn.getOverlap() > 0 && currentColumn.getOverlap() >= minLocalActivity);
    return currentColumn.isActive();
  }

  /**
   * WP
   * SPATIAL POOLING
   * <p/>
   * Phase 3: Learning
   * The third phase performs learning; it updates the permanence values of all synapses as necessary, as well as the boost and inhibition radius.
   * <p/>
   * First part
   * The main learning rule is implemented in lines 20-26. For winning columns, if a synapse is active, its permanence value is incremented, otherwise it is decremented. Permanence values are constrained to be between 0 and 1.
   */

  public void learnSpatialForActiveForColumn(Column currentColumn) {
    if (currentColumn.isActive()) {
      List<Synapse.ProximalSynapse> potentialSynapses = currentColumn.getPotentialSynapses();
      for (Synapse.ProximalSynapse potentialSynapse : potentialSynapses) {
        if (potentialSynapse.getConnectedSensoryInput().getValue()) {
          potentialSynapse.setPermanence(
                  potentialSynapse.getPermanence() + Synapse.ProximalSynapse.PERMANENCE_INCREASE);
        } else {
          potentialSynapse.setPermanence(
                  potentialSynapse.getPermanence() - Synapse.ProximalSynapse.PERMANENCE_DECREASE);
        }
      }
    }
  }

  /**
   * Second part
   * Lines 28-36 implement boosting. There are two separate boosting mechanisms in place to help a column learn connections. If a column does not win often enough (as measured by activeDutyCycle), its overall boost value is increased (line 30-32). Alternatively, if a column's connected synapses do not overlap well with any inputs often enough (as measured by overlapDutyCycle), its permanence values are boosted (line 34-36). Note: once learning is turned off, boost(c) is frozen.
   * Finally, at the end of Phase 3 the inhibition radius is recomputed (line 38).
   *
   * @param inhibitionRadius
   */
  public void boostWeakForColumn(Column currentColumn, double inhibitionRadius) {
    double minDutyCycle = 0.01 * currentColumn.getMaxDutyCycle(inhibitionRadius);
    currentColumn.updateBoost(minDutyCycle, this.getBoostRate());
    if (currentColumn.getOverlapDutyCycle() < minDutyCycle) {
      currentColumn.increasePermanence(0.1 * Synapse.ProximalSynapse.CONNECTED_PERMANENCE);
    }
  }


  /**
   * WP
   * kthScore(cols, k)
   * Given the list of columns, return the k'th highest overlap value.
   */
  private double kthScore(List<Column> neighbors, int desiredLocalActivity) {
    Collections.sort(neighbors, OVERLAP_COMPARATOR);
    if (desiredLocalActivity > neighbors.size()) {
      desiredLocalActivity = neighbors.size();
    }
    return neighbors.get(desiredLocalActivity - 1).getOverlap();

  }


}
