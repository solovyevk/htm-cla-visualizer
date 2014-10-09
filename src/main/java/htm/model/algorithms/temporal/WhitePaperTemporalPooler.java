/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.algorithms.temporal;

import htm.model.*;
import htm.utils.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class WhitePaperTemporalPooler extends TemporalPooler {

  private static final Log LOG = LogFactory.getLog(WhitePaperTemporalPooler.class);

  public WhitePaperTemporalPooler(Config cfg) {
    super(cfg);
  }


  /**
   * Performs temporal pooling based on the current spatial pooler output.
   * WP:
   * The input to this code is activeColumns(t), as computed by the spatial pooler.
   * The code computes the active and predictive state for each cell at the current
   * time step, t. The boolean OR of the active and predictive states for each cell
   * forms the output of the temporal pooler for the next level.
   * <p/>
   * Phase 1:
   * Compute the active state, activeState(t), for each cell.
   * The first phase calculates the activeState for each cell that is in a winning column.
   * For those columns, the code further selects one cell per column as the learning cell (learnState).
   * The logic is as follows: if the bottom-up input was predicted by any cell
   * (i.e. its predictiveState output was 1 due to a sequence segment),
   * then those cells become active (lines 23-27). If that segment became
   * active from cells chosen with learnState on, this cell is selected as the learning cell (lines 28-30).
   * If the bottom-up input was not predicted, then all cells in the become active (lines 32-34).
   * In addition, the best matching cell is chosen as the learning cell (lines 36-41) and a
   * new segment is added to that cell.
   * <p/>
   * Phase 2:
   * Compute the predicted state, predictiveState(t), for each cell.
   * The second phase calculates the predictive state for each cell.
   * A cell will turn on its predictive state output if one of its segments becomes active,
   * i.e. if enough of its lateral inputs are currently active due to feed-forward input.
   * In this case, the cell queues up the following changes:
   * a) reinforcement of the currently active segment (lines 47-48), and
   * b) reinforcement of a segment that could have predicted this activation, i.e. a segment that has a (potentially weak)
   * match to activity during the previous time step (lines 50-53).
   * <p/>
   * Phase 3:
   * Update synapses. The third and last phase actually carries out learning. In this
   * phase segmentUpdates updates that have been queued up are actually implemented
   * once we get feed-forward input and the cell is chosen as a learning cell
   * (lines 56-57). Otherwise, if the cell ever stops predicting for any reason, we
   */
  @Override public void execute() {
    nextTimeStep();
    phaseOne();
    phaseTwo();
    phaseThree();
  }

  public void phaseOne() {
    //Phase 1:Compute the active state, activeState(t), for each cell.
    List<Column> activeColumns = layer.getActiveColumns();
    for (Column activeColumn : activeColumns) {
      computeCellsActiveStateForColumn(activeColumn);
    }
  }

  public void phaseTwo() {
    //Phase 2:Compute the predicted state, predictiveState(t), for each cell.
    for (Column column : layer.getElementsList()) {
      computeCellsPredictiveStateForColumn(column);
    }
  }


  public void phaseThree() {
    //Phase 3:Run synapses updates accumulated in previous steps
    if (isLearningMode()) {
      for (Column column : layer.getElementsList()) {
        updateDistalSynapsesForColumn(column);
      }
    }
  }


  /*Reset cells*/
  public void nextTimeStep() {
    for (Column column : layer.getElementsList()) {
      column.nextTimeStep();
    }
  }

  /**
   * WP
   * TEMPORAL POOLING
   * <p/>
   * Phase 1: Compute cells state
   * The first phase calculates the activeState for each cell that is in a winning column.
   * For those columns, the code further selects one cell per column as the learning cell (learnState).
   * The logic is as follows: if the bottom-up input was predicted by any cell (i.e. its predictiveState
   * output was 1 due to a sequence segment), then those cells become active (lines 23-27).
   * If that segment became active from cells chosen with learnState on, this cell is selected as
   * the learning cell (lines 28-30). If the bottom-up input was not predicted, then all cells
   * in the become active (lines 32-34). In addition, the best matching cell is chosen as the
   * learning cell (lines 36-41) and a new segment is added to that cell.
   */
  public void computeCellsActiveStateForColumn(Column currentColumn) {
    if (!currentColumn.isActive()) {
      throw new RuntimeException("Column should be active");
    }
    boolean buPredicted = false, lcChosen = false;
    for (Cell cell : currentColumn.getElementsList()) {
      if (cell.getPredictiveState(Cell.BEFORE)) {
        DistalDendriteSegment segment = getActiveSegment(cell, Cell.BEFORE, Cell.State.ACTIVE);
        if (segment != null && segment.isSequenceSegment()) {
          buPredicted = true;
          cell.setActiveState(true);
          if (segment.segmentActive(Cell.BEFORE, Cell.State.LEARN, this.getActivationThreshold())) {
            lcChosen = true;
            cell.setLearnState(true);
            break;
          }
        }
      }
    }
    if (!buPredicted) {
      for (Cell cell : currentColumn.getElementsList()) {
        cell.setActiveState(true);
      }
    }
    if (!lcChosen && this.isLearningMode()) {
      //TODO we need to consider only seq segments when looking for the best segment here in phase 1 - this is for new connections only, can't select future activated segments here
      BestMatchingCellAndSegment bestMatchingCellAndSegment = getBestMatchingCell(currentColumn, Cell.BEFORE);
      Cell bestCell = bestMatchingCellAndSegment.getCell();
      DistalDendriteSegment learningCellBestSegment = bestMatchingCellAndSegment.getSegment();
      bestCell.setLearnState(true);
      // segmentUpdate is added internally to the bestCell's update list.
      DistalDendriteSegment.Update segmentUpdate = getSegmentActiveSynapses(bestCell, learningCellBestSegment,
                                                                            Cell.BEFORE, true, null);
    }
  }


  /**
   * Phase: 2
   * <p/>
   * WP
   * TEMPORAL POOLING
   * <p/>
   * The second phase calculates the predictive state for each cell.
   * A cell will turn on its predictive state output if one of its
   * segments becomes active, i.e. if enough of its lateral inputs are currently active due to feed-forward input.
   * In this case, the cell queues up the following changes: a) reinforcement of the currently
   * active segment (lines 47-48), and b) reinforcement of a segment that could have predicted this activation,
   * i.e. a segment that has a (potentially weak) match to activity during the previous time step (lines 50-53).
   */
  public void computeCellsPredictiveStateForColumn(Column currentColumn) {
    for (Cell cell : currentColumn.getElementsList()) {
      for (DistalDendriteSegment segment : cell.getSegments()) {
        if (segment.segmentActive(Cell.NOW, Cell.State.ACTIVE, this.getActivationThreshold())) {
          //By Kirill - if segment is seq it also should be in learning state to predict
          if (segment.isSequenceSegment() && !segment.segmentActive(Cell.NOW, Cell.State.LEARN, this.getActivationThreshold())) {
            continue;
          }
          //By Kirill
            /*Cell can't be predicted if in learning state, otherwise it's learning state will case
            adding segments update from phase 1 & 2 in following phase 3 within the same step, but we don't know if the cell will be active in next step
            ALSO same said in WP - page 30, Temporal pooler details paragraph 2)
            Cells with active dendrite segments are put in the predictive state unless they are already active due to feed-forward input
             */
          if (cell.getLearnState(Cell.NOW)) {
            continue;
          }
          cell.setPredictInStepState(segment.predictedInStep());
          if (isLearningMode()) {
            DistalDendriteSegment.Update activeUpdate = getSegmentActiveSynapses(cell, segment, Cell.NOW, false,
                                                                                 segment.getPredictedBy());
            DistalDendriteSegment.Update previousUpdate = getSegmentActiveSynapses(cell, getBestMatchingSegment(cell,
                                                                                                                Cell.BEFORE),
                                                                                   Cell.BEFORE, true, segment);
          }
        }
      }
    }

  }

  /**
   * Phase: 3
   * <p/>
   * WP
   * TEMPORAL POOLING
   * <p/>
   * The third and last phase actually carries out learning.
   * In this phase segment updates that have been queued up are actually
   * implemented once we get feed-forward input and the cell is chosen as
   * a learning cell (lines 56-57). Otherwise, if the cell ever stops predicting
   * for any reason, we negatively reinforce the segments (lines 58-60).
   */
  public void updateDistalSynapsesForColumn(Column currentColumn) {
    for (Cell cell : currentColumn.getElementsList()) {
      if (cell.getLearnState(Cell.NOW)) {
        adaptSegments(cell, true);
      } else if (cell.getPredictiveState(Cell.BEFORE) && !cell.getPredictiveState(Cell.NOW)) {
        adaptSegments(cell, false);
      } else if (cell.getPredictInStepState(Cell.NOW) >= cell.getPredictInStepState(
              Cell.BEFORE) && cell.getPredictInStepState(Cell.BEFORE) != Cell.NOT_IN_STEP_PREDICTION) {
        adaptSegmentsForWrongPrediction(cell);
      }
    }
  }

  /**
   * WP
   * <p/>
   * getActiveSegment(c, i, t, state)
   * <p/>
   * For the given column c cell i, return a segment index such that segmentActive(s,t, state) is true.
   * If multiple segments are active, sequence segments are given preference.
   * Otherwise, segments with most activity are given preference.
   */

  public DistalDendriteSegment getActiveSegment(Cell cell, final int time, final Cell.State state) {
    final TemporalPooler self = this;
    List<DistalDendriteSegment> activeSegments =
            CollectionUtils.filter(cell.getElementsList(), new CollectionUtils.Predicate<DistalDendriteSegment>() {
              @Override
              public boolean apply(
                      DistalDendriteSegment segment) {
                return segment.segmentActive(time, state, self.getActivationThreshold());
              }
            });
    Collections.sort(activeSegments, new Comparator<DistalDendriteSegment>() {
      @Override
      public int compare(DistalDendriteSegment segment, DistalDendriteSegment segmentToCompare) {
        int amountActiveCells = segment.getConnectedWithStateCell(time, state).size();
        int amountActiveCellsToCompare = segmentToCompare.getConnectedWithStateCell(time, state).size();
        if (segment.isSequenceSegment() == segmentToCompare.isSequenceSegment()
            && amountActiveCells == amountActiveCellsToCompare) {
          return 0;
        } else if ((segment.isSequenceSegment() && !segmentToCompare.isSequenceSegment())
                   || (segment.isSequenceSegment() == segmentToCompare.isSequenceSegment()
                       && amountActiveCells > amountActiveCellsToCompare)) {
          return 1;
        } else {
          return -1;
        }
      }
    });
    return activeSegments.size() > 0 ? activeSegments.get(activeSegments.size() - 1) : null;
  }


  /**
   * WP
   * <p/>
   * getBestMatchingCell(c)
   * For the given column, return the cell with the best matching segment.
   * If no cell has a matching segment, then return the cell with the fewest number of segments.
   *
   * @param time
   * @return
   */

  protected BestMatchingCellAndSegment getBestMatchingCell(Column currentColumn, int time) {
    List<DistalDendriteSegment> bestMatchingSegmentsFromCells = new ArrayList<DistalDendriteSegment>();
    boolean allSegmentsCreated = true;
    for (Cell cell : currentColumn.getElementsList()) {
      if (cell.getSegments().size() == 0) {
        allSegmentsCreated = false;
        break;
      }
    }
    Cell minSegmentListCell = currentColumn.getElementsList().get(0);
    for (Cell cell : currentColumn.getElementsList()) {
      //By Kirill
      //Avoid selecting learning cell as best matching to make sure we use next cell in column to help with temporal forking
      if (cell.getLearnState(time)) {
        //Shift minCell to next, since we exclude this one
        int nextInx = cell.getCellIndex() + 1;
        if (nextInx < currentColumn.getElementsList().size()) {
          minSegmentListCell = currentColumn.getElementByIndex(nextInx);
        } else if (getBestMatchingSegment(cell, time) == null) {
          //LOG.warn("Possible repeating pattern, please increase number of cells in column");
        }
        continue;
      }
      if (cell.getSegments().size() < minSegmentListCell.getSegments().size()) {
        minSegmentListCell = cell;
      }
      DistalDendriteSegment bestMatchingSegment = getBestMatchingSegment(cell, time);
      if (bestMatchingSegment != null) {
        bestMatchingSegmentsFromCells.add(bestMatchingSegment);
      }
    }
    DistalDendriteSegment columnBestMatchingSegment = getBestMatchingSegment(bestMatchingSegmentsFromCells,
                                                                             time);

    return new WhitePaperTemporalPooler.BestMatchingCellAndSegment(
            columnBestMatchingSegment != null ? columnBestMatchingSegment.getOwner() : minSegmentListCell,
            columnBestMatchingSegment);
  }

  /**
   * WP
   * <p/>
   * getBestMatchingSegment(c, i, t)
   * <p/>
   * For the given column c cell i at time t, find the segment with the largest number of active synapses.
   * This routine is aggressive in finding the best match. The permanence value of synapses is allowed to be
   * below connectedPerm. The number of active synapses is allowed to be below activationThreshold,
   * but must be above minThreshold. The routine returns the segment index. If no segments are found, then an index of -1 is returned.
   *
   * @param time
   * @return
   */
  public DistalDendriteSegment getBestMatchingSegment(Cell currentCell, final int time) {
    return getBestMatchingSegment(new ArrayList<DistalDendriteSegment>(currentCell.getElements()), time);
  }

  public DistalDendriteSegment getBestMatchingSegment(List<DistalDendriteSegment> segmentList, final int time) {

    Collections.sort(segmentList, new Comparator<DistalDendriteSegment>() {
      @Override
      public int compare(DistalDendriteSegment segment, DistalDendriteSegment segmentToCompare) {
        int amountActiveCells = segment.getActiveCellSynapses(time).size();
        int amountActiveCellsToCompare = segmentToCompare.getActiveCellSynapses(time).size();
        if (amountActiveCells == amountActiveCellsToCompare) {
          return 0;
        } else if (amountActiveCells > amountActiveCellsToCompare) {
          return 1;
        } else {
          return -1;
        }
      }
    });
    return segmentList.size() > 0 && segmentList.get(segmentList.size() - 1).getActiveCellSynapses(
            time).size() > this.getMinThreshold() ? segmentList.get(segmentList.size() - 1) : null;
  }

  /**
   * If the segment is NULL, then a new segment is to be added, otherwise
   * the specified segment is updated.  If the segment exists, find all active
   * synapses for the segment (either at t or t-1)
   * and mark them as needing to be updated.  If newSynapses is true, then
   * Region.newSynapseCount - len(activeSynapses) new synapses are added to the
   * segment to be updated.  The (new) synapses are randomly chosen from the set
   * of current learning cells (within Region.predictionRadius if set).
   * These segment updates are only applied when the applySegmentUpdates
   * method is later called on this Cell.
   * <p/>
   * WP
   * <p/>
   * getSegmentActiveSynapses(c, i, t, s, newSynapses= false)
   * <p/>
   * Return a segmentUpdate data structure containing a list of proposed changes to segment s.
   * Let activeSynapses be the list of active synapses where the originating cells have their
   * activeState output = 1 at time step t. (This list is empty if s = -1 since the segment doesn't exist.)
   * newSynapses is an optional argument that defaults to false. If newSynapses is true,
   * then newSynapseCount - count(activeSynapses) synapses are added to activeSynapses.
   * These synapses are randomly chosen from the set of cells that have learnState output = 1 at time step t.
   */
  public DistalDendriteSegment.Update getSegmentActiveSynapses(Cell currentCell, DistalDendriteSegment segment,
                                                               int time,
                                                               boolean newSynapses, DistalDendriteSegment predictedBy) {
    DistalDendriteSegment.Update result = new DistalDendriteSegment.Update(currentCell, segment, time, predictedBy);
    if (segment != null) {
      result.addAll(segment.getActiveCellSynapses(time));
    }
    int numberOfNewSynapsesToAdd = this.getNewSynapseCount() - result.getElementsList().size();
    if (newSynapses && numberOfNewSynapsesToAdd > 0) {
      List<Column> neighbors = currentCell.getNeighborsAndMyColumn();
      List<Cell> cellWithLearnStateList = new ArrayList<Cell>();
      //TODO Refac

      for (Column neighborColumn : neighbors) {
        List<Cell> cellList = neighborColumn.getElements();
        for (Cell cell : cellList) {
            /*NOTE: There is no indication in the Numenta pseudocode that a cell shouldn't be able to have a
            *distal synapse from another cell in the same column. Therefore the below check is commented out.
            * Skip cells in our own col (don't connect to ourself)
            * */
          //if (cell.belongsToColumn == this.belongsToColumn) {
          //  continue;
          // }
           /*But avoid self reverence*/
          if (cell == currentCell) {
            continue;
          }
          if (cell.getLearnState(time)) {
            cellWithLearnStateList.add(cell);
          }
        }
      }
      Collections.shuffle(cellWithLearnStateList);
      numberOfNewSynapsesToAdd = cellWithLearnStateList.size() < numberOfNewSynapsesToAdd ? cellWithLearnStateList.size() : numberOfNewSynapsesToAdd;
      for (int i = 0; i < numberOfNewSynapsesToAdd; i++) {
        Cell cellWithLearnState = cellWithLearnStateList.get(i);
        result.addElement(new Synapse.DistalSynapse(cellWithLearnState));
      }
    }
    currentCell.fireUpdatesChange();
    return result;
  }

  /**
   * WP
   * <p/>
   * adaptSegments(segmentList, positiveReinforcement)
   * <p/>
   * This function iterates through a list of segmentUpdate's and reinforces each segment.
   * For each segmentUpdate element, the following changes are performed.
   * If positiveReinforcement is true then synapses on the active list get
   * their permanence counts incremented by permanenceInc. All other synapses
   * get their permanence counts decremented by permanenceDec. If positiveReinforcement
   * is false, then synapses on the active list get their permanence counts decremented by permanenceDec.
   * After this step, any synapses in segmentUpdate that do yet exist get added with a permanence count of initialPerm.
   */
  public void adaptSegments(Cell currentCell, boolean positiveReinforcement) {
    for (DistalDendriteSegment.Update segmentUpdate : currentCell.getSegmentUpdates()) {
      DistalDendriteSegment segment;
      //Only create new segment if there are synapses and reinforcement is positive
      if (segmentUpdate.isNewSegment() && segmentUpdate.size() > 0 && positiveReinforcement) {
        segment = new DistalDendriteSegment(currentCell, segmentUpdate.getPredictedBy());
      } else {
        segment = segmentUpdate.getTarget();
      }
      if (segment != null) {
        for (Synapse.DistalSynapse distalSynapse : segment.getElementsList()) {
          if (positiveReinforcement) {
            if (segmentUpdate.contains(distalSynapse)) {
              distalSynapse.setPermanence(distalSynapse.getPermanence() + Synapse.DistalSynapse.PERMANENCE_INCREASE);
            } else {
              //distalSynapse.setPermanence(distalSynapse.getPermanence() - Synapse.DistalSynapse.PERMANENCE_DECREASE);
              //By Kirill - only decrease permanence if no column shared
              List<Cell> columnCells = distalSynapse.getFromCell().getOwner().getElements();
              boolean keep = false;
              for (Cell columnCell : columnCells) {
                for (Synapse.DistalSynapse synapse : segmentUpdate.getElementsList()) {
                  if (synapse.getFromCell() == columnCell) {
                    keep = true;
                    break;
                  }
                }
                if (keep) break;
              }
              if (!keep) {
                distalSynapse.setPermanence(distalSynapse.getPermanence() - Synapse.DistalSynapse.PERMANENCE_DECREASE);
              }
            }
          } else {
            if (segmentUpdate.contains(distalSynapse)) {
              distalSynapse.setPermanence(distalSynapse.getPermanence() - Synapse.DistalSynapse.PERMANENCE_DECREASE);
            }
          }
        }
        for (Synapse.DistalSynapse distalSynapse : segmentUpdate.getElementsList()) {
          if (!segment.contains(distalSynapse)) {
            segment.addElement(distalSynapse);
          }
        }
      }
      //DELETE processed segmentUpdate
      //this.segmentUpdates.remove(segmentUpdate);
    }
    //Clear segmentUpdates after adaption;
    currentCell.getSegmentUpdates().clear();
    currentCell.fireUpdatesChange();
    //fireSegmentsChange();
  }

  public void adaptSegmentsForWrongPrediction(Cell currentCell) {
    int currentStepCount = currentCell.getPredictInStepState(
            Cell.NOW);
    LOG.warn("Prediction is wrong for Cell:" + this + "\n" + "Before predicted in " + currentCell.getPredictInStepState(
            Cell.BEFORE) + " step(s); Now predicted in " + currentCell.getPredictInStepState(Cell.NOW) + " step(s)");
    //, trim  segment:" + trimSegment);
    LOG.info("Negatively reinforce and remove segmentUpdates created before or at " + currentStepCount + " step(s)");
     /*Michael Ferrier fix #1 - http://sourceforge.net/p/openhtm/discussion/htm/thread/ccedad1f/?limit=25&page=4
     apply only those segment updates created before or at that given creation time,
     and removes those from the list*/
     /*try {
      Thread.sleep(1000 * 20);
     } catch (Exception e) {
       LOG.error("Process sleep interrupted", e);
     }*/
    for (ListIterator<DistalDendriteSegment.Update> iter = currentCell.getSegmentUpdates().listIterator(
            currentCell.getSegmentUpdates().size()); iter.hasPrevious(); ) {
      DistalDendriteSegment.Update update = iter.previous();
      if (update.predictedInStep() <= currentStepCount) {
        int predictedInStep = update.predictedInStep();
        LOG.info("Decrement synapses and remove segment update for step prediction: " + predictedInStep + update);
        for (Synapse.DistalSynapse distalSynapse : update.getElementsList()) {
          distalSynapse.setPermanence(distalSynapse.getPermanence() - 4 * Synapse.DistalSynapse.PERMANENCE_DECREASE);
        }
        iter.remove();
      }
    }
  }

  public static class BestMatchingCellAndSegment {
    private final Cell cell;
    private final DistalDendriteSegment segment;

    private BestMatchingCellAndSegment(Cell bestCell, DistalDendriteSegment bestSegment) {
      this.cell = bestCell;
      this.segment = bestSegment;
    }

    public Cell getCell() {
      return cell;
    }

    public DistalDendriteSegment getSegment() {
      return segment;
    }
  }

  /**
    * Number of step until column will be predicted in temporal sequence/step(s)
    *
    * @return
    */
   public int isColumnPredictInStep(Column currentColumn) {
     for (Cell cell : currentColumn.getElementsList()) {
       if (cell.getPredictiveState(Cell.NOW)) {
         return cell.getPredictInStepState(Cell.NOW);
       }
     }
     return Cell.NOT_IN_STEP_PREDICTION;
   }
}
