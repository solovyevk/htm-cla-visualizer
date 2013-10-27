/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model;

import htm.visualizer.utils.CircularArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Column {
  public static int CELLS_PER_COLUMN = 3;
  private static final int COLUMN_MAX_ACTIVE = 1000;
  private final int columnIndex;
  private final List<Cell> cells = new ArrayList<Cell>();
  private ArrayList<Boolean> activeList = new CircularArrayList<Boolean>(COLUMN_MAX_ACTIVE);

  /**
   * activeDutyCycle(c) A sliding average representing how often column c has
   * been active after inhibition (e.g. over the last 1000 iterations).
   */
  private double activeDutyCycle;

  public Column(int columnIndex) {
    this.columnIndex = columnIndex;
    for (int i = 0; i < CELLS_PER_COLUMN; i++) {
      cells.add(new Cell(this, i));
    }
  }

  public int getColumnIndex() {
    return columnIndex;
  }

  public List<Cell> getCells() {
    return Collections.unmodifiableList(cells);
  }

  public Cell getCellByIndex(int index){
    return cells.get(index);
  }

  /**
      * updateActiveDutyCycle(c) Computes a moving average of how often column c
      * has been active after inhibition.
      *
      * @return
      */
     private double updateActiveDutyCycle() {
         int totalActive = 0;
         for (boolean act : activeList) {
             if (act) {
                 totalActive++;
             }
         }
         this.activeDutyCycle = (double) totalActive / activeList.size();
         return activeDutyCycle;
     }

     public void setActive(boolean active) {
         // logger.log(Level.INFO, "activeList" + activeList.size());
         activeList.add(0, active);
         updateActiveDutyCycle();
     }

     public boolean isActive() {
         return this.activeList.size() > 0 ? activeList.get(0) : false;
     }
}
