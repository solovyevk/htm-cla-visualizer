package htm.visualizer;

import htm.model.*;
import htm.utils.UIUtils;
import htm.visualizer.surface.BaseSurface;
import htm.visualizer.surface.ColumnSDRSurface;
import htm.visualizer.surface.RegionSlicedHorizontalView;
import htm.visualizer.surface.SensoryInputSurface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.*;

public class HTMGraphicInterface extends JPanel {
  private static final Log LOG = LogFactory.getLog(HTMGraphicInterface.class);
  /*
  Parameters below will be used if no valid config provided
   */

  /*
  Default HTM Region Parameters
   */
  private static final int HORIZONTAL_COLUMN_NUMBER = 12;
  private static final int VERTICAL_COLUMN_NUMBER = 12;
  private static final int SENSORY_INPUT_WIDTH = 12;
  private static final int SENSORY_INPUT_HEIGHT = 12;
  private static final double INPUT_RADIUS = 8;
  private static final double LEARNING_RADIUS = 4;
  private static final int CELLS_PER_COLUMN = 3;


  /*
  Default HTM Column Parameters
   */

  private static final int DESIRED_LOCAL_ACTIVITY = 1;
  private static final int MINIMAL_OVERLAP = 2;
  private static final int AMOUNT_OF_PROXIMAL_SYNAPSES = 30;
  private static final double BOOST_RATE = 0.01;

  /*
  Default HTM Cell Parameters
  */
  private static final int NEW_SYNAPSE_COUNT = 5;
  private static final int ACTIVATION_THRESHOLD = 2;
  private static final int MIN_THRESHOLD = 0;//1;
  private static final int AMOUNT_OF_DISTAL_SYNAPSES = 30;
  private static final int TIME_STEPS = 6;

  /*
  Default Proximal Synapse Parameters
  */

  public static double PROXIMAL_SYNAPSE_CONNECTED_PERMANENCE = 0.2;
  public static double PROXIMAL_SYNAPSE_PERMANENCE_INCREASE = 0.005;
  public static double PROXIMAL_SYNAPSE_PERMANENCE_DECREASE = 0.005;

  /*
  Default Distal Synapse Parameters
  */

  public static double DISTAL_SYNAPSE_CONNECTED_PERMANENCE = 0.2;
  public static double DISTAL_SYNAPSE_PERMANENCE_INCREASE = 0.005;
  public static double DISTAL_SYNAPSE_PERMANENCE_DECREASE = 0.005;


  static {
    Column.BOOST_RATE = BOOST_RATE;
  }

  private java.util.List<boolean[]> patterns = new ArrayList<boolean[]>();


  private HTMProcess process;


  private Region region;
  private final RegionSlicedHorizontalView slicedView;
  private final ControlPanel control;
  private final SensoryInputSurface sensoryInputSurface;
  private final ColumnSDRSurface sdrInput;
  private SpatialInfo spatialInfo;
  private TemporalInfo temporalInfo;
  private SelectedDetails detailsInfo;
  //Need this to ensure sliced view update before region cells reset for next step
  private volatile CountDownLatch viewsUpdateLatch = new CountDownLatch(0);


  public HTMGraphicInterface() {
    this(new Config(null, new Region.Config(new Dimension(HORIZONTAL_COLUMN_NUMBER, VERTICAL_COLUMN_NUMBER),
                                            new Dimension(SENSORY_INPUT_WIDTH, SENSORY_INPUT_HEIGHT), INPUT_RADIUS,
                                            LEARNING_RADIUS,
                                            false, CELLS_PER_COLUMN),
                    new Column.Config(AMOUNT_OF_PROXIMAL_SYNAPSES,
                                      MINIMAL_OVERLAP, DESIRED_LOCAL_ACTIVITY, BOOST_RATE),
                    new Cell.Config(NEW_SYNAPSE_COUNT, ACTIVATION_THRESHOLD, MIN_THRESHOLD, AMOUNT_OF_DISTAL_SYNAPSES,
                                    TIME_STEPS),
                    new Synapse.Config(PROXIMAL_SYNAPSE_CONNECTED_PERMANENCE, PROXIMAL_SYNAPSE_PERMANENCE_INCREASE,
                                       PROXIMAL_SYNAPSE_PERMANENCE_DECREASE),
                    new Synapse.Config(DISTAL_SYNAPSE_CONNECTED_PERMANENCE, DISTAL_SYNAPSE_PERMANENCE_INCREASE,
                                       DISTAL_SYNAPSE_PERMANENCE_DECREASE)));
  }

  public HTMGraphicInterface(Config cfg) {
    super(new BorderLayout(0, 0));
    //Set static attributes for HTM Model classes
    Column.updateFromConfig(cfg.getColumnConfig());
    Cell.updateFromConfig(cfg.getCellConfig());
    Synapse.ProximalSynapse.updateFromConfig(cfg.getProximalSynapseConfig());
    Synapse.DistalSynapse.updateFromConfig(cfg.getDistalSynapseConfig());
    //Initialize region and all related UI
    this.region = new Region(cfg.getRegionConfig());
    this.sensoryInputSurface = new SensoryInputSurface(region.getInputSpace());
    this.sdrInput = new ColumnSDRSurface(region);
    this.slicedView = new RegionSlicedHorizontalView(region) {
      @Override
      public Dimension getPreferredSize() {
        int prefWidth = super.getPreferredSize().width;
        return new Dimension(prefWidth, 270 * region.getCellsInColumn());
      }
    };
    this.control = new ControlPanel();
    if (!region.isSkipSpatial()) {
      this.spatialInfo = new SpatialInfo();
    }
    this.temporalInfo = new TemporalInfo(region);
    this.detailsInfo = new SelectedDetails(spatialInfo, temporalInfo);
    initLayout();
    initProcess();
    initListeners();
    if (cfg.patterns != null && cfg.patterns.size() > 0) {
      setPatterns(cfg.patterns);
    }
    LOG.debug("Finish initialization");
  }


  public java.util.List<boolean[]> getPatterns() {
    return patterns;
  }

  public void setPatterns(List<boolean[]> patterns) {
    this.patterns = patterns;
    process.sendUpdateNotification();
  }


  private void initProcess() {
    process = new HTMProcess();
    process.addObserver(control);
    /*Repaint after each step*/
    final JComponent win = this;
    process.addObserver(new Observer() {
      @Override public void update(Observable o, Object arg) {
        temporalInfo.getRegionColumnsVerticalView().updateColumns();
        LOG.debug("Repaint Window on PROCESS update");
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            LOG.debug("Start Painted View in step:#" + process.getCycle() + ", " + process.currentPatternIndex);
            win.repaint();
          }
        });
      }
    });
  }

  private void initListeners() {
    //select cell to view temporal details
    slicedView.addElementMouseEnterListener(new BaseSurface.ElementMouseEnterListener() {
      @Override
      public void onElementMouseEnter(BaseSurface.ElementMouseEnterEvent e) {
        detailsInfo.getTabs().setSelectedComponent(temporalInfo);
        Cell cell = ((RegionSlicedHorizontalView.ColumnCellsByIndexSurface)e.getSource()).getCell(e.getIndex());
        LOG.debug("Cell was clicked:" + cell);
        temporalInfo.setCurrentCell(cell);
      }
    });
    //backward selection from active columns temporal info info to Region Slice;
    temporalInfo.getRegionColumnsVerticalView().addElementMouseEnterListener(
            new BaseSurface.ElementMouseEnterListener() {
              @Override
              public void onElementMouseEnter(BaseSurface.ElementMouseEnterEvent e) {
                Cell selectedCell = temporalInfo.getRegionColumnsVerticalView().getCell(e.getIndex());
                slicedView.setClickedOnCell(selectedCell);
                temporalInfo.setCurrentCell(selectedCell);
              }
            });
    //backward selection from selected synapse on temporal info to Region Slice;
    temporalInfo.getSegmentDistalSynapsesTable().getSelectionModel().addListSelectionListener(new SynapseTableSelectListener(temporalInfo.getSegmentDistalSynapsesTable(), slicedView));
    temporalInfo.getSegmentUpdateDistalSynapsesTable().getSelectionModel().addListSelectionListener(new SynapseTableSelectListener(temporalInfo.getSegmentUpdateDistalSynapsesTable(),slicedView));
    //backward selection from selected segment on temporal info to Region Slice;
    temporalInfo.getDistalDendriteSegmentsTable().getSelectionModel().addListSelectionListener(new SegmentTableSelectListener(temporalInfo.getDistalDendriteSegmentsTable(), slicedView));
    temporalInfo.getDistalDendriteSegmentUpdatesTable().getSelectionModel().addListSelectionListener(new SegmentTableSelectListener(temporalInfo.getDistalDendriteSegmentUpdatesTable(), slicedView));
    if (!region.isSkipSpatial()) {
      //select column to view spatial details
      sdrInput.addElementMouseEnterListener(new BaseSurface.ElementMouseEnterListener() {
        @Override
        public void onElementMouseEnter(BaseSurface.ElementMouseEnterEvent e) {
          detailsInfo.getTabs().setSelectedComponent(spatialInfo);
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
            int inputIndex = (Integer)spatialInfo.getNeighborColumnsTable().getModel().getValueAt(rowColumnModelInx, 6);
            sdrInput.setSelectedColumn(inputIndex);
          }
        }
      });
    }
  }

  private abstract static class TableSelectListener implements ListSelectionListener {
     protected final JTable sourceTable;
     protected final RegionSlicedHorizontalView slicedView;

     private TableSelectListener(JTable sourceTable, RegionSlicedHorizontalView slicedView) {
       this.sourceTable = sourceTable;
       this.slicedView = slicedView;
     }
   }

  private static class SegmentTableSelectListener extends TableSelectListener {

    private SegmentTableSelectListener(JTable sourceTable, RegionSlicedHorizontalView slicedView) {
      super(sourceTable, slicedView);
    }

    @Override public void valueChanged(ListSelectionEvent e) {
      selectFromSegment();
    }

    private void selectFromSegment(){
      int rowViewInx = sourceTable.getSelectedRow();
       if (rowViewInx == -1) {
         slicedView.setSelectedSegment(null);
       } else {
         int rowColumnModelInx = sourceTable.convertRowIndexToModel(
                 rowViewInx);
         DistalDendriteSegment selectedSegment = ((TemporalInfo.SegmentsModel)sourceTable.getModel()).getSegment(
                 rowColumnModelInx);
         slicedView.setSelectedSegment(selectedSegment);
         slicedView.repaint();
       }
    }
  }

  private static class SynapseTableSelectListener extends TableSelectListener {
    private SynapseTableSelectListener(JTable sourceTable, RegionSlicedHorizontalView slicedView) {
      super(sourceTable, slicedView);
    }

    @Override public void valueChanged(ListSelectionEvent e) {
      selectFromSynapse();
    }

    private void selectFromSynapse() {
      int rowViewInx = sourceTable.getSelectedRow();
      if (rowViewInx == -1) {
        slicedView.setSelectedSynapseCellPosition(null);
      } else {
        int rowColumnModelInx = sourceTable.convertRowIndexToModel(
                rowViewInx);
        Synapse.DistalSynapse selectedSynapse = ((TemporalInfo.BaseSegmentDistalSynapsesModel)sourceTable.getModel()).getSynapse(
                rowColumnModelInx);
        slicedView.setSelectedSynapse(selectedSynapse);
        slicedView.repaint();
      }
    }
  }


  private void initLayout() {
    this.add(new Container() {
      private Container init() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.weightx = 2;// + Column.CELLS_PER_COLUMN * .1;
        this.add(new Container() {
          private Container init() {
            this.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.NORTH;
            c.fill = GridBagConstraints.BOTH;
            this.add(control, c);
            c.weighty = 1.55;
            c.weightx = 1.0;
            c.gridy = 1;
            this.add(new JComponent() {

              @Override public void paint(Graphics g) {
                super.paint(g);
                viewsUpdateLatch.countDown();
              }

              private Container init() {
                this.setLayout(new GridLayout(0, 2, 10, 10));
                this.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Sensory Input & SD Representation"),
                        UIUtils.DEFAULT_BORDER));

                sensoryInputSurface.setBorder(UIUtils.LIGHT_GRAY_BORDER);
                add(sensoryInputSurface);
                sdrInput.setBorder(UIUtils.LIGHT_GRAY_BORDER);
                add(sdrInput);
                return this;
              }
            }.init(), c);
            c.gridy = 2;
            c.weighty = 0.45;
            this.add(detailsInfo, c);
            return this;
          }
        }.init(), c);
        c.weightx = 1.5;
        slicedView.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Region Slices"),
                UIUtils.DEFAULT_BORDER));
        JScrollPane sp = new JScrollPane(slicedView) {
          @Override public void paint(Graphics g) {
            super.paint(g);
            viewsUpdateLatch.countDown();
          }
        };
        sp.setBorder(UIUtils.DEFAULT_BORDER);
        this.add(sp, c);
        return this;
      }
    }.init(), BorderLayout.CENTER);
  }

  Region getRegion() {
    return region;
  }


  Config getParameters() {
    return new Config(patterns, new Region.Config(region.getDimension(), region.getInputSpaceDimension(),
                                                  region.getInputRadius(), region.getLearningRadius(),
                                                  region.isSkipSpatial(), region.getCellsInColumn()),
                      new Column.Config(Column.AMOUNT_OF_PROXIMAL_SYNAPSES,
                                        Column.MIN_OVERLAP,
                                        Column.DESIRED_LOCAL_ACTIVITY, Column.BOOST_RATE),
                      new Cell.Config(Cell.NEW_SYNAPSE_COUNT,
                                      Cell.ACTIVATION_THRESHOLD,
                                      Cell.MIN_THRESHOLD,
                                      Cell.AMOUNT_OF_SYNAPSES,
                                      Cell.TIME_STEPS),
                      new Synapse.Config(Synapse.ProximalSynapse.CONNECTED_PERMANENCE,
                                         Synapse.ProximalSynapse.PERMANENCE_INCREASE,
                                         Synapse.ProximalSynapse.PERMANENCE_DECREASE
                      ),
                      new Synapse.Config(Synapse.DistalSynapse.CONNECTED_PERMANENCE,
                                         Synapse.DistalSynapse.PERMANENCE_INCREASE,
                                         Synapse.DistalSynapse.PERMANENCE_DECREASE
                      ));
  }


  private static class SelectedDetails extends JPanel {
    private JTabbedPane tabs;

    public JTabbedPane getTabs() {
      return tabs;
    }

    public SelectedDetails(SpatialInfo spatialInfo, TemporalInfo temporalInfo) {
      super(new BorderLayout());
      setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Selected Column/Cell Detail Info"),
              UIUtils.DEFAULT_BORDER));
      tabs = new JTabbedPane();
      if (spatialInfo != null) {
        tabs.addTab("Spatial Column Info", spatialInfo);
      }
      tabs.addTab("Temporal Cell Info", temporalInfo);
      tabs.setBorder(UIUtils.LIGHT_GRAY_BORDER);
      tabs.setBackground(Color.WHITE);
     /* JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                            top, tabs);
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
      this.add(splitPane, BorderLayout.CENTER);*/
      this.add(tabs);
    }
  }

  private class ControlPanel extends JPanel implements Observer {
    /*
 Controls
  */
    private Action addPatternAction;
    private Action resetPatternsAction;
    private Action runAction;
    private Action stepAction;
    private Action stopAction;
    private Action cleanInputSpaceAction;
    private Action spatialLearningAction;
    private Action temporalLearningAction;

    final JToolBar toolBar = new JToolBar();
    final Container infoPane = new Container();
    private JLabel pattersInfo = new JLabel("Patterns: 0");
    private JLabel stepInfo = new JLabel("Current Pattern: 0");
    private JLabel cycleInfo = new JLabel("Cycle: 0");

    @Override public void update(Observable o, Object arg) {
      enableActions();
      infoPane.setVisible(patterns.size() > 0);
      pattersInfo.setText("Patterns: " + patterns.size() + "");
      stepInfo.setText("Current: " + process.getCurrentPatternIndex() + "");
      cycleInfo.setText("Cycle: " + process.getCycle());
    }

    private void enableActions() {
      resetPatternsAction.setEnabled(patterns.size() > 0);
      runAction.setEnabled(patterns.size() > 0 && !process.isRunning());
      stepAction.setEnabled(patterns.size() > 0);
      stopAction.setEnabled(patterns.size() > 0 && process.isRunning());
    }


    private void initActions() {
      cleanInputSpaceAction = new AbstractAction("Clean", UIUtils.INSTANCE.createImageIcon(
              "/images/cleanup.png")) {
        @Override public void actionPerformed(ActionEvent e) {
          sensoryInputSurface.reset();
        }
      };

      addPatternAction = new AbstractAction("Add", UIUtils.INSTANCE.createImageIcon(
              "/images/add.png")) {
        @Override public void actionPerformed(ActionEvent e) {
          addPattern();
        }

      };

      resetPatternsAction = new AbstractAction("Reset", UIUtils.INSTANCE.createImageIcon(
              "/images/refresh.png")) {
        @Override public void actionPerformed(ActionEvent e) {
          resetPatterns();
        }
      };

      runAction = new AbstractAction("Run", UIUtils.INSTANCE.createImageIcon(
              "/images/play.png")) {
        @Override public void actionPerformed(ActionEvent e) {
          process.run();
        }
      };

      stepAction = new AbstractAction("Step", UIUtils.INSTANCE.createImageIcon(
              "/images/step.png")) {

        @Override public void actionPerformed(ActionEvent e) {
          process.step();
        }

      };

      stopAction = new AbstractAction("Stop", UIUtils.INSTANCE.createImageIcon(
              "/images/stop.png")) {
        @Override public void actionPerformed(ActionEvent e) {
          process.stop();
        }
      };

      spatialLearningAction = new AbstractAction("Learn Spat.") {
            @Override public void actionPerformed(ActionEvent e) {
              region.setSpatialLearning(!region.getSpatialLearning());
            }

      };
      spatialLearningAction.putValue(Action.SELECTED_KEY, region.getSpatialLearning());

      temporalLearningAction = new AbstractAction("Learn Temp.") {
              @Override public void actionPerformed(ActionEvent e) {
                region.setTemporalLearning(!region.getTemporalLearning());
              }

        };

      temporalLearningAction.putValue(Action.SELECTED_KEY, region.getTemporalLearning());

      enableActions();

    }


    public ControlPanel() {
      initActions();
      infoPane.setPreferredSize(new Dimension(250, infoPane.getPreferredSize().height));
      infoPane.setLayout(new GridLayout(0, 3, 1, 1));
      infoPane.setVisible(patterns.size() > 0);
      infoPane.add(pattersInfo);
      infoPane.add(stepInfo);
      infoPane.add(cycleInfo);
      toolBar.add(new JButton(cleanInputSpaceAction));
      toolBar.add(new JButton(addPatternAction));
      toolBar.add(new JButton(resetPatternsAction));
      toolBar.add(new JButton(runAction));
      toolBar.add(new JButton(stepAction));
      toolBar.add(new JButton(stopAction));
      toolBar.add(new JCheckBox(spatialLearningAction));
      toolBar.add(new JCheckBox(temporalLearningAction));
      /*
      toolBar.add(new JButton(new AbstractAction("test") {
        @Override public void actionPerformed(ActionEvent e) {
          List<Column> selectedColumns = region.getActiveColumns(2);
          for (Column selectedColumn : selectedColumns) {
            selectedColumn.setMarked(true);
          }
          sdrInput.repaint();
        }
      }));*/
      toolBar.addSeparator();
      toolBar.add(infoPane);
      this.add(toolBar);
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(400,
                           40);
    }

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }


  }


  /*Control Methods*/
  private void addPattern() {
    patterns.add(sensoryInputSurface.getSensoryInputValues());
    region.nextTimeStep();
    region.performSpatialPooling();
    region.performTemporalPooling();
    process.sendUpdateNotification();
    this.repaint();
  }

  private void resetPatterns() {
    patterns.clear();
    sensoryInputSurface.reset();
    process.reset();
  }

  private class HTMProcess extends Observable {
    private int currentPatternIndex = 0;
    private int cycleCounter = 0;
    private ExecutorService es = Executors.newSingleThreadExecutor();
    private Future<Boolean> processFuture;

    public void sendUpdateNotification() {
      setChanged();
      notifyObservers();
    }


    public boolean step() {
      if (patterns.size() != 0) {
        sensoryInputSurface.setSensoryInputValues(patterns.get(currentPatternIndex));
        try {
          viewsUpdateLatch.await();
        } catch (InterruptedException e) {
          LOG.error("viewsUpdateLatch interrupted", e);
        }
        LOG.debug("Start step #" + process.getCycle() + ", " + process.currentPatternIndex);
        region.nextTimeStep();
        region.performSpatialPooling();
        region.performTemporalPooling();
//        try {
//          Thread.sleep(20);
//        } catch (Exception e) {
//          LOG.error("Process sleep interrupted", e);
//        }
        if (currentPatternIndex < patterns.size() - 1) {
          currentPatternIndex++;
        } else {
          cycleCounter++;
          currentPatternIndex = 0;
        }
        viewsUpdateLatch = new CountDownLatch(2);
        sendUpdateNotification();
        return true;
      } else {
        return false;
      }
    }

    public void run() {
      processFuture = es.submit(new Callable<Boolean>() {
          @Override
          public Boolean call() {
              if (patterns.size() == 0) {
                return false;
              }

              while(!processFuture.isCancelled()){
                step();
              }
              return false;
          }
      });
    }

    public void stop() {
      if(processFuture != null)
        processFuture.cancel(true);
    }

    public boolean isRunning() {
      if(processFuture != null)
        return !processFuture.isDone();
      else
        return false;
    }

    public int getCurrentPatternIndex() {
      return currentPatternIndex;
    }

    public int getCycle() {
      return cycleCounter;
    }

    public void reset() {
      processFuture = null;
      currentPatternIndex = 0;
      cycleCounter = 0;
      sendUpdateNotification();
    }
  }




  public static class Config {
    private final java.util.List<boolean[]> patterns;
    private final Region.Config regionConfig;
    private final Column.Config columnConfig;
    private final Cell.Config cellConfig;
    private final Synapse.Config proximalSynapseConfig;
    private final Synapse.Config distalSynapseConfig;


    public Config(List<boolean[]> patterns, Region.Config regionConfig, Column.Config columnConfig,
                  Cell.Config cellConfig,
                  Synapse.ProximalSynapse.Config proximalSynapseConfig,
                  Synapse.DistalSynapse.Config distalSynapseConfig) {
      this.patterns = patterns;
      this.regionConfig = regionConfig;
      this.columnConfig = columnConfig;
      this.cellConfig = cellConfig;
      this.proximalSynapseConfig = proximalSynapseConfig;
      this.distalSynapseConfig = distalSynapseConfig;
    }

    public Region.Config getRegionConfig() {
      return regionConfig;
    }

    public List<boolean[]> getPatterns() {
      return patterns;
    }

    public Column.Config getColumnConfig() {
      return columnConfig;
    }

    public Synapse.Config getProximalSynapseConfig() {
      return proximalSynapseConfig;
    }

    public Synapse.Config getDistalSynapseConfig() {
      return distalSynapseConfig;
    }

    public Cell.Config getCellConfig() {
      return cellConfig;
    }


  }

}



