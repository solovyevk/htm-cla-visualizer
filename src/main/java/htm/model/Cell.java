/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model;

import htm.visualizer.utils.CircularArrayList;

public class Cell {

  public static final int ACTIVE_STATE = 1;
  public static final int LEARN_STATE = 2;

  public static final int BEFORE = 1;
  public static final int NOW = 0;

  /**
   * cell will keep a buffer of its last TIME_STEPS output values
   */
  private static int TIME_STEPS = 2;

  private final Column belongsToColumn;
  private final int cellIndex;
  /**
   * Boolean vector of Cell's active state in time t-n, ..., t-1, t
   */
  private CellStateBuffer activeState = new CellStateBuffer();

  /**
   * Boolean vector of Cell's predictive state in time t-n, ..., t-1, t
   */
  private CellStateBuffer predictiveState = new CellStateBuffer();
  /**
   * learnState(c, i, t) A boolean indicating whether cell i in column c is
   * chosen as the cell to learn on.
   */
  private CellStateBuffer learnState = new CellStateBuffer();

  public Cell(Column belongsToColumn, int cellIndex) {
    this.belongsToColumn = belongsToColumn;
    this.cellIndex = cellIndex;
  }

   /*
  *Set Learn State in current time Cell.NOW
   */
  public void setLearnState(boolean learnState) {
    this.learnState.setState(learnState);
  }

  /**
   * Get Learn state in Time
   *
   * @param time
   */
  public boolean getLearnState(int time) {
    return this.learnState.get(time);
  }

  /**
   * Set active state
   */
  public void setActiveState(boolean activeState) {
    this.activeState.setState(activeState);
  }

  /**
   * Get Active state in Time
   *
   * @param time
   */
  public boolean getActiveState(int time) {
    return this.activeState.get(time);
  }

  /**
   * Set Predictive state
   */
  public void setPredictiveState(boolean predictiveState) {
    this.predictiveState.setState(predictiveState);
  }

  /**
   * Get Predictive state in Time
   *
   * @param time
   */
  public boolean getPredictiveState(int time) {
    return this.predictiveState.get(time);
  }

  private static class CellStateBuffer extends CircularArrayList<Boolean>{
    public CellStateBuffer() {
      super(TIME_STEPS);
      for (int i = 0; i < TIME_STEPS; i++) {
         this.add(false);
      }
    }

    /**
    * Set the current state(time NOW)
    *
    * @param state
    **/
    void setState(boolean state){
      this.set(NOW, state);
    }
  }
}
