package htm.visualizer;

import htm.model.Cell;
import htm.model.Column;
import htm.model.Region;
import htm.model.space.InputSpace;
import htm.visualizer.surface.BaseSurface;
import htm.visualizer.surface.ColumnCellsByIndexSurface;
import htm.visualizer.surface.ColumnSDRSurface;
import htm.visualizer.surface.SensoryInputSurface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class HTMGraphicInterface extends JPanel {
  private static final Log LOG = LogFactory.getLog(HTMGraphicInterface.class);
  /*
  Global HTM Region Parameters
   */
  private static final int HORIZONTAL_COLUMN_NUMBER = 12;
  private static final int VERTICAL_COLUMN_NUMBER = 12;
  private static final int SENSORY_INPUT_WIDTH = 12;
  private static final int SENSORY_INPUT_HEIGHT = 12;
  private static final int CELLS_PER_COLUMN = 3;

  //TODO move them to region
  /*
  SP Parameters
   */
  private static final int SP_DESIRED_LOCAL_ACTIVITY = 3;
  private static final int SP_MINIMAL_OVERLAP = 3;
  private static final int SP_AMOUNT_OF_SYNAPSES = 20;

  /*inputRadius for this input Space
  * The concept of Input Radius is an additional parameter to control how
  * far away synapse connections can be made instead of allowing connections anywhere.
  */
  private static final double SP_INPUT_RADIUS = 5;
  /*The amount that is added to a Column's Boost value in a single time step, when it is being boosted.*/
  private static final double SP_BOOST_RATE = 0.01;


  private static Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(0, 4, 0, 4);
  private static Border LIGHT_GRAY_BORDER = BorderFactory.createLineBorder(Color.lightGray);

  static {
    Column.CELLS_PER_COLUMN = CELLS_PER_COLUMN;
    Column.AMOUNT_OF_PROXIMAL_SYNAPSES = SP_AMOUNT_OF_SYNAPSES;
    Column.MIN_OVERLAP = SP_MINIMAL_OVERLAP;
    Column.DESIRED_LOCAL_ACTIVITY = SP_DESIRED_LOCAL_ACTIVITY;
    Column.BOOST_RATE = SP_BOOST_RATE;
  }

  private java.util.List<boolean[]> patterns = new ArrayList<boolean[]>();


  private HTMProcess process;

  /*
  Controls
   */
  private Action addPatternAction;
  private Action resetPatternsAction;
  private Action runAction;
  private ObservableAction stepAction;
  private Action stopAction;


  private InputSpace sensoryInput;
  private Region region;
  private final JComponent slicedView;
  private final ControlPanel control;
  private final SensoryInputSurface sensoryInputSurface;
  private final ColumnSDRSurface sdrInput;
  private final SpatialInfo spatialInfo;


  public HTMGraphicInterface() {
    this(new Config(null, new Dimension(HORIZONTAL_COLUMN_NUMBER, VERTICAL_COLUMN_NUMBER),
                    new Dimension(SENSORY_INPUT_WIDTH, SENSORY_INPUT_HEIGHT), SP_INPUT_RADIUS));
  }

  public HTMGraphicInterface(Config cfg) {
    super(new BorderLayout(0, 0));
    initActions();
    this.sensoryInput = new InputSpace(cfg.sensoryInputDimension.width, cfg.sensoryInputDimension.height);
    this.sensoryInputSurface = new SensoryInputSurface(sensoryInput);
    this.region = new Region(cfg.regionDimension.width, cfg.regionDimension.height, sensoryInput, cfg.inputRadius);
    this.sdrInput = new ColumnSDRSurface(region);
    this.slicedView = new HTMRegionSlicedView();
    this.control = new ControlPanel();
    this.spatialInfo = new SpatialInfo();
    initLayout();
    initProcess();
    initListeners();
    if(cfg.patterns != null && cfg.patterns.size() > 0){
      setPatterns(cfg.patterns);
    }
    LOG.debug("Finish initialization");
  }

  private void initActions() {
   // final HTMGraphicInterface win = this;


      /*
      Controls
       */
    addPatternAction = new AbstractAction("Add Pattern") {
      @Override public void actionPerformed(ActionEvent e) {
        addPattern();
        StringBuilder newName = new StringBuilder("Add Pattern");
        this.putValue(NAME, newName.append("(").append(patterns.size()).append(")").toString());
      }

    };

    resetPatternsAction = new AbstractAction("Reset Patterns") {
      @Override public void actionPerformed(ActionEvent e) {
        resetPatterns();
      }
    };

    runAction = new AbstractAction("Run") {
      @Override public void actionPerformed(ActionEvent e) {
        process.run();
      }
    };

    stepAction = new ObservableAction("Step") {

      @Override public void actionPerformed(ActionEvent e) {
        process.step();
      }

      @Override public void update(Observable o, Object currentPatternIndex) {
        StringBuffer newName = new StringBuffer("Step");
        this.putValue(NAME, newName.append(" #").append(process.getCurrentPatternIndex() + 1).toString());
      }
    };

    stopAction = new AbstractAction("Stop") {
      @Override public void actionPerformed(ActionEvent e) {
        process.stop();
      }
    };

  }


  public java.util.List<boolean[]> getPatterns() {
    return patterns;
  }

  public void setPatterns(List<boolean[]> patterns) {
    this.patterns = patterns;
    sensoryInputSurface.setSensoryInputValues(patterns.get(0));
    addPatternAction.putValue(Action.NAME, new StringBuilder("Add Pattern").append("(").append(patterns.size()).append(")").toString());
  }

  private abstract static class ObservableAction extends AbstractAction implements Observer {
    protected ObservableAction(String name) {
      super(name);
    }
  }


  private void initProcess() {
    process = new HTMProcess();
    process.addObserver(stepAction);
    /*Repaint after each step*/
    final JComponent win = this;
    process.addObserver(new Observer() {
      @Override public void update(Observable o, Object arg) {
        win.repaint();
      }
    });
  }

  private void initListeners() {
    //select column to view details
    sdrInput.addElementMouseEnterListener(new BaseSurface.ElementMouseEnterListener() {
      @Override public void onElementMouseEnter(BaseSurface.ElementMouseEnterEvent e) {
        int index = e.getIndex();
        Column column = sdrInput.getColumn(index);
        sensoryInputSurface.setCurrentColumn(column);
        sdrInput.setCurrentColumn(column);
        spatialInfo.setCurrentColumn(column);
      }
    });
    //backward selection from synapses spatial info to Input Space
    spatialInfo.getSynapsesTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override public void valueChanged(ListSelectionEvent e) {
        int rowViewInx = spatialInfo.getSynapsesTable().getSelectedRow();
        if (rowViewInx == -1) {
          sensoryInputSurface.setSelectedInput(null);
        } else {
          int rowColumnModelInx = spatialInfo.getSynapsesTable().convertRowIndexToModel(rowViewInx);
          int inputIndex = (Integer)spatialInfo.getSynapsesTable().getModel().getValueAt(rowColumnModelInx, 3);
          sensoryInputSurface.setSelectedInput(inputIndex);
        }
      }
    });
    //backward selection from neighbors columns spatial info to SDR Space
    spatialInfo.getNeighborColumnsTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override public void valueChanged(ListSelectionEvent e) {
        int rowViewInx = spatialInfo.getNeighborColumnsTable().getSelectedRow();
        if (rowViewInx == -1) {
          sensoryInputSurface.setSelectedInput(null);
        } else {
          int rowColumnModelInx = spatialInfo.getNeighborColumnsTable().convertRowIndexToModel(rowViewInx);
          int inputIndex = (Integer)spatialInfo.getNeighborColumnsTable().getModel().getValueAt(rowColumnModelInx, 3);
          sdrInput.setSelectedColumn(inputIndex);
        }
      }
    });
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
            c.weighty = 1.5;
            c.weightx = 1.0;
            JComponent bottom = new SelectedCellsAndDetails(spatialInfo);
            c.gridy = 1;
            this.add(new JComponent() {
              private Container init() {
                this.setLayout(new GridLayout(0, 2, 10, 10));
                this.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Sensory Input & SD Representation"),
                        DEFAULT_BORDER));

                sensoryInputSurface.setBorder(LIGHT_GRAY_BORDER);
                add(sensoryInputSurface);
                sdrInput.setBorder(LIGHT_GRAY_BORDER);
                add(sdrInput);
                return this;
              }
            }.init(), c);
            c.gridy = 2;
            c.weighty = 0.5;
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

  Region getRegion(){
    return region;
  }

  InputSpace getSensoryInput(){
    return sensoryInput;
  }


  private static class SelectedCellsAndDetails extends JPanel {
    public SelectedCellsAndDetails(SpatialInfo columnInfo) {
      super(new BorderLayout());
      setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Selected/Active Column & Details"),
              DEFAULT_BORDER));
      final SensoryInputSurface top = new SensoryInputSurface(5, 3);
      top.setBorder(LIGHT_GRAY_BORDER);
      JComponent test = new JPanel();
      test.setBackground(Color.WHITE);
      final JTabbedPane bottom = new JTabbedPane();
      bottom.addTab("Spatial Info", columnInfo);
      bottom.addTab("Temporal Info", test);
      bottom.setBorder(LIGHT_GRAY_BORDER);
      bottom.setBackground(Color.WHITE);
      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                            top, bottom);
      splitPane.setOneTouchExpandable(true);
      splitPane.setDividerLocation(0);
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
      //TODO remove
      JButton test = new JButton("test");
      test.addActionListener(new ActionListener() {
        @Override public void actionPerformed(ActionEvent e) {
          Collection<InputSpace.Input> r = sensoryInputSurface.getSensoryInput().getAllWithinRadius(new Point(5, 5), 3);
          for (InputSpace.Input input : r) {
            sensoryInputSurface.getSensoryInput().setInputValue(input.getIndex(), true);
          }
          sensoryInputSurface.repaint();
          LOG.debug("AverageReceptiveFieldSize:" + region.getAverageReceptiveFieldSize());
        }
      });
      add(test);


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
        final BaseSurface cellLayer = new ColumnCellsByIndexSurface(region.getDimension().width, region.getDimension().height,
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
    patterns.add(sensoryInputSurface.getSensoryInputValues());
    //sensoryInputSurface.reset();
  }

  private void resetPatterns() {
    patterns.clear();
    sensoryInputSurface.reset();
    process.reset();
    addPatternAction.putValue(Action.NAME, "Add Pattern");
    stepAction.putValue(Action.NAME, "Step");
  }

  private class HTMProcess extends Observable {
    private boolean running = false;
    private int currentPatternIndex = 0;

    public boolean step() {
      if (patterns.size() != 0) {
        sensoryInputSurface.setSensoryInputValues(patterns.get(currentPatternIndex));
        region.performSpatialPooling();
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

  public static class Config {

    private final java.util.List<boolean[]> patterns;
    private final Dimension regionDimension;
    private final Dimension sensoryInputDimension;
    private final double inputRadius;

    public Config(List<boolean[]> patterns, Dimension regionDimension, Dimension sensoryInputDimension,
                  double inputRadius) {
      this.patterns = patterns;
      this.regionDimension = regionDimension;
      this.sensoryInputDimension = sensoryInputDimension;
      this.inputRadius = inputRadius;
    }


  }

}



