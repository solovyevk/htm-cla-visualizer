/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer;

import htm.model.Cell;
import htm.model.Column;
import htm.model.Region;
import htm.visualizer.surface.BaseSurface;
import htm.visualizer.surface.ColumnCellsByIndexSurface;
import htm.visualizer.surface.ColumnSDRSurface;
import htm.visualizer.surface.SensoryInputSurface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class HTMGraphicInterface extends JPanel {
  private static final Log LOG = LogFactory.getLog(HTMGraphicInterface.class);
  /*
  Global HTM Region Parameters
   */
  private static final int HORIZONTAL_COLUMN_NUMBER = 12;
  private static final int VERTICAL_COLUMN_NUMBER = 12;
  private static final int CELLS_PER_COLUMN = 3;

  //TODO move them to region
  /*
  SP Parameters
   */
  private static final int SP_DESIRED_LOCAL_ACTIVITY = 1;
  private static final double SP_CONNECTED_PERMANENCE = 0.7;
  private static final int SP_MINIMAL_OVERLAP = 2;
  private static final double SP_PERMANENCE_DEC = 0.05;
  private static final double SP_PERMANENCE_INC = 0.05;
  private static final int SP_AMOUNT_OF_SYNAPSES = 60;
  private static final double SP_INHIBITION_RADIUS = 5.0;


  private static Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(0, 4, 0, 4);
  private static Border LIGHT_GRAY_BORDER = BorderFactory.createLineBorder(Color.lightGray);

  static {
    Column.CELLS_PER_COLUMN = CELLS_PER_COLUMN;
  }

  private ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();

  private Region region = new Region(HORIZONTAL_COLUMN_NUMBER, VERTICAL_COLUMN_NUMBER);
  private HTMProcess process;

  /*
  Controls
   */
  private Action addPatternAction = new AbstractAction("Add Pattern") {
    @Override public void actionPerformed(ActionEvent e) {
      addPattern();
      StringBuffer newName = new StringBuffer("Add Pattern");
      this.putValue(NAME, newName.append("(").append(patterns.size()).append(")").toString());
    }

  };

  private Action resetPatternsAction = new AbstractAction("Reset Patterns") {
    @Override public void actionPerformed(ActionEvent e) {
      resetPatterns();
    }
  };

  private Action runAction = new AbstractAction("Run") {
    @Override public void actionPerformed(ActionEvent e) {
      process.run();
    }
  };

  private abstract static class ObservableAction extends AbstractAction implements Observer {
    protected ObservableAction(String name) {
      super(name);
    }
  }

  private ObservableAction stepAction = new ObservableAction("Step") {

    @Override public void actionPerformed(ActionEvent e) {
      process.step();
    }

    @Override public void update(Observable o, Object currentPatternIndex) {
      StringBuffer newName = new StringBuffer("Step");
      this.putValue(NAME, newName.append(" #").append(process.getCurrentPatternIndex() + 1).toString());
    }
  };

  private Action stopAction = new AbstractAction("Stop") {
    @Override public void actionPerformed(ActionEvent e) {
      process.stop();
    }
  };

  private final JComponent slicedView = new HTMRegionSlicedView();
  private final ControlPanel control = new ControlPanel();
  private final SelectedCellsAndDetails details = new SelectedCellsAndDetails();
  private final SensoryInputSurface sensoryInput =  new SensoryInputSurface(HORIZONTAL_COLUMN_NUMBER,
                                                                                              VERTICAL_COLUMN_NUMBER);

  private final ColumnSDRSurface sdrInput = new ColumnSDRSurface(HORIZONTAL_COLUMN_NUMBER,
                                                                                  VERTICAL_COLUMN_NUMBER, region.getColumns());


  public HTMGraphicInterface() {
    super(new BorderLayout(0, 0));
    initLayout();
    initProcess();
    LOG.debug("Finish initialization");
  }

  private void initProcess() {
    process = new HTMProcess();
    process.addObserver(stepAction);
  }

  private void initLayout() {
    this.add(new Container() {
      private Container init() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.weightx = 3 + CELLS_PER_COLUMN * .1;
        this.add(new Container() {
          private Container init() {
            this.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.NORTH;
            c.fill = GridBagConstraints.BOTH;
            this.add(control, c);
            c.weighty = 1.0;
            c.weightx = 1.0;
            JComponent bottom = new SelectedCellsAndDetails();
            c.gridy = 1;
            this.add(new JComponent() {
              private Container init() {
                this.setLayout(new GridLayout(0, 2, 10, 10));
                this.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Sensory Input & SD Representation"),
                        DEFAULT_BORDER));

                sensoryInput.setBorder(LIGHT_GRAY_BORDER);
                add(sensoryInput);
                sdrInput.setBorder(LIGHT_GRAY_BORDER);
                add(sdrInput);
                return this;
              }
            }.init(), c);
            c.gridy = 2;
            c.weighty = 1.0;
            this.add(bottom, c);
            return this;
          }
        }.init(), c);
        c.weightx = 1.5;
        JScrollPane sp = new JScrollPane(slicedView);
        sp.setBorder(DEFAULT_BORDER);
        this.add(sp, c);
        return this;
      }
    }.init(), BorderLayout.CENTER);
  }


  private static class SelectedCellsAndDetails extends JPanel {
    public SelectedCellsAndDetails() {
      super(new BorderLayout());
      setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Selected/Active Column & Details"),
              DEFAULT_BORDER));
      final SensoryInputSurface top = new SensoryInputSurface(5, 3);
      top.setBorder(LIGHT_GRAY_BORDER);
      final JComponent bottom = new JPanel();
      bottom.setBorder(LIGHT_GRAY_BORDER);
      bottom.setBackground(Color.WHITE);
      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                            top, bottom);
      splitPane.setOneTouchExpandable(true);
      splitPane.setDividerLocation(150);
      //TODO REVIEW THIS
      splitPane.setUI(new BasicSplitPaneUI() {
        public BasicSplitPaneDivider createDefaultDivider() {
          return new BasicSplitPaneDivider(this) {
            public void setBorder(Border b) {
            }
          };
        }
      });
      splitPane.setBorder(null);
      this.add(splitPane, BorderLayout.CENTER);
    }
  }

  private class ControlPanel extends JPanel {
    public ControlPanel() {
      setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Controls"),
              DEFAULT_BORDER));
      add(new JButton(addPatternAction));
      add(new JButton(resetPatternsAction));
      add(new JButton(runAction));
      add(new JButton(stepAction));
      add(new JButton(stopAction));


    }

    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    public Dimension getPreferredSize() {
      return new Dimension(super.getPreferredSize().width,
                           60);
    }

    public Dimension getMaximumSize() {
      return getPreferredSize();
    }


  }


  private class HTMRegionSlicedView extends JPanel {
    public HTMRegionSlicedView() {
      super(new GridLayout(CELLS_PER_COLUMN, 0));
      setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Region Slices"),
              DEFAULT_BORDER));
      Column[] columns = region.getColumns();
      for (int i = 0; i < Column.CELLS_PER_COLUMN; i++) {
        Cell[] layer = new Cell[columns.length];
        for (int j = 0; j < columns.length; j++) {
          layer[j] = columns[j].getCellByIndex(i);
        }
        final BaseSurface cellLayer = new ColumnCellsByIndexSurface(HORIZONTAL_COLUMN_NUMBER, VERTICAL_COLUMN_NUMBER,
                                                                    layer);
        cellLayer.setBorder(LIGHT_GRAY_BORDER);
        cellLayer.addElementMouseEnterListener(new BaseSurface.ElementMouseEnterListener() {
          @Override public void onElementMouseEnter(BaseSurface.ElementMouseEnterEvent e) {
            Cell cell = ((ColumnCellsByIndexSurface)e.getSource()).getCell(e.getIndex());
            System.out.println("Cell was clicked:" + cell);
          }
        });
        this.add(new Container() {
          private Container init(String caption) {
            this.setLayout(new BorderLayout());
            this.add(new JLabel(caption), BorderLayout.NORTH);
            this.add(cellLayer, BorderLayout.CENTER);
            return this;
          }
        }.init("Layer #" + i));
      }
    }


    public Dimension getPreferredSize() {
      return new Dimension(super.getPreferredSize().width,
                           200 * CELLS_PER_COLUMN);
    }

  }


  /*Control Methods*/
  private void addPattern() {
    patterns.add(sensoryInput.getSensoryInput());
    sensoryInput.reset();
  }

  private void resetPatterns() {
    patterns.clear();
    sensoryInput.reset();
    process.reset();
    addPatternAction.putValue(Action.NAME, "Add Pattern");
    stepAction.putValue(Action.NAME, "Step");
  }

  private class HTMProcess extends Observable {
    private boolean running = false;
    private int currentPatternIndex = 0;

    public boolean step() {
      if (patterns.size() != 0) {
        sensoryInput.setSensoryInput(patterns.get(currentPatternIndex));
        try {
          Thread.sleep(500);
        } catch (Exception e) {
          System.out.println("fucked");
        }
        if (currentPatternIndex < patterns.size() - 1) {
          currentPatternIndex++;
        } else {
          currentPatternIndex = 0;
        }
        setChanged();
        notifyObservers(currentPatternIndex);
        return true;
      } else {
        return false;
      }
    }

    public void run() {
      new Thread(new Runnable() {
        @Override public void run() {
          if (patterns.size() != 0) {
            running = true;
          }
          do {
            step();
          } while (running);
        }
      }).start();

    }

    public void stop() {
      running = false;
    }

    public boolean isRunning() {
      return running;
    }

    public int getCurrentPatternIndex() {
      return currentPatternIndex;
    }

    public void reset() {
      running = false;
      currentPatternIndex = 0;
      notifyObservers();
    }
  }

}



