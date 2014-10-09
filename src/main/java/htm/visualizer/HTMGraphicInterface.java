package htm.visualizer;

import htm.model.*;
import htm.model.algorithms.temporal.TemporalPooler;
import htm.model.algorithms.temporal.WhitePaperTemporalPooler;
import htm.utils.UIUtils;
import htm.visualizer.surface.BaseSurface;
import htm.visualizer.surface.ColumnSDRSurface;
import htm.visualizer.surface.LayerSlicedHorizontalView;
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
  Default HTM Cell/TemporalPooler Parameters
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

  private final Object temporalSplitLock = new Object();

  private HTMProcess process;

  private WhitePaperTemporalPooler temporalPooler;
  private Layer layer;
  private final LayerSlicedHorizontalView slicedView;
  private final ControlPanel control;
  private final SensoryInputSurface sensoryInputSurface;
  private final ColumnSDRSurface sdrInput;
  private SpatialInfo spatialInfo;
  private TemporalInfo temporalInfo;
  private SelectedDetails detailsInfo;
  //Need this to ensure sliced view update before layer cells reset for next step
  private volatile CountDownLatch viewsUpdateLatch = new CountDownLatch(0);


  public HTMGraphicInterface() {
    this(new Config(null,
                    new TemporalPooler.Config(NEW_SYNAPSE_COUNT,
                                              ACTIVATION_THRESHOLD,
                                              MIN_THRESHOLD),
                    new Layer.Config(new Dimension(HORIZONTAL_COLUMN_NUMBER, VERTICAL_COLUMN_NUMBER),
                                     new Dimension(SENSORY_INPUT_WIDTH, SENSORY_INPUT_HEIGHT), INPUT_RADIUS,
                                     LEARNING_RADIUS,
                                     false, CELLS_PER_COLUMN),
                    new Column.Config(AMOUNT_OF_PROXIMAL_SYNAPSES,
                                      MINIMAL_OVERLAP, DESIRED_LOCAL_ACTIVITY, BOOST_RATE),
                    new Cell.Config(AMOUNT_OF_DISTAL_SYNAPSES,
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
    //Initialize layer and all related UI
    this.layer = new Layer(cfg.getRegionConfig());
    this.temporalPooler = (WhitePaperTemporalPooler)new WhitePaperTemporalPooler(cfg.getTemporalPoolerConfig()).setLayer(layer);
    this.sensoryInputSurface = new SensoryInputSurface(layer.getInputSpace());
    this.sdrInput = new ColumnSDRSurface(layer);
    this.slicedView = new LayerSlicedHorizontalView(layer) {
      @Override
      public Dimension getPreferredSize() {
        int prefWidth = super.getPreferredSize().width;
        return new Dimension(prefWidth, 270 * layer.getCellsInColumn());
      }
    };
    this.control = new ControlPanel();
    if (!layer.isSkipSpatial()) {
      this.spatialInfo = new SpatialInfo();
    }
    this.temporalInfo = new TemporalInfo(layer);
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
        LOG.debug("Repaint Window on PROCESS update");
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            LOG.debug("Start Painted View in step:#" + process.getCycle() + ", " + process.currentPatternIndex);
            temporalInfo.getRegionColumnsVerticalView().updateColumns();
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
        Cell cell = ((LayerSlicedHorizontalView.ColumnCellsByIndexSurface)e.getSource()).getCell(e.getIndex());
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
    temporalInfo.getSegmentDistalSynapsesTable().getSelectionModel().addListSelectionListener(
            new SynapseTableSelectListener(temporalInfo.getSegmentDistalSynapsesTable(), slicedView));
    temporalInfo.getSegmentUpdateDistalSynapsesTable().getSelectionModel().addListSelectionListener(
            new SynapseTableSelectListener(temporalInfo.getSegmentUpdateDistalSynapsesTable(), slicedView));
    //backward selection from selected segment on temporal info to Region Slice;
    temporalInfo.getDistalDendriteSegmentsTable().getSelectionModel().addListSelectionListener(
            new SegmentTableSelectListener(temporalInfo.getDistalDendriteSegmentsTable(), slicedView));
    temporalInfo.getDistalDendriteSegmentUpdatesTable().getSelectionModel().addListSelectionListener(
            new SegmentTableSelectListener(temporalInfo.getDistalDendriteSegmentUpdatesTable(), slicedView));
    if (!layer.isSkipSpatial()) {
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
    protected final LayerSlicedHorizontalView slicedView;

    private TableSelectListener(JTable sourceTable, LayerSlicedHorizontalView slicedView) {
      this.sourceTable = sourceTable;
      this.slicedView = slicedView;
    }
  }

  private static class SegmentTableSelectListener extends TableSelectListener {

    private SegmentTableSelectListener(JTable sourceTable, LayerSlicedHorizontalView slicedView) {
      super(sourceTable, slicedView);
    }

    @Override public void valueChanged(ListSelectionEvent e) {
      selectFromSegment();
    }

    private void selectFromSegment() {
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
    private SynapseTableSelectListener(JTable sourceTable, LayerSlicedHorizontalView slicedView) {
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
            GridBagConstraints c1 = new GridBagConstraints();
            c1.gridx = 0;
            c1.gridy = 0;
            c1.anchor = GridBagConstraints.NORTH;
            c1.fill = GridBagConstraints.BOTH;
            this.add(control, c1);
            c1.weighty = 1.55;
            c1.weightx = 1.0;
            c1.gridy = 1;
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
            }.init(), c1);
            c1.gridy = 2;
            c1.weighty = 0.45;
            this.add(detailsInfo, c1);
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

  Layer getLayer() {
    return layer;
  }


  Config getParameters() {
    return new Config(patterns, new WhitePaperTemporalPooler.Config(temporalPooler.getNewSynapseCount(),
                                                          temporalPooler.getActivationThreshold(),
                                                          temporalPooler.getMinThreshold()),
                      new Layer.Config(layer.getDimension(), layer.getInputSpaceDimension(),
                                       layer.getInputRadius(), layer.getLearningRadius(),
                                       layer.isSkipSpatial(), layer.getCellsInColumn()),

                      new Column.Config(Column.AMOUNT_OF_PROXIMAL_SYNAPSES,
                                        Column.MIN_OVERLAP,
                                        Column.DESIRED_LOCAL_ACTIVITY, Column.BOOST_RATE),
                      new Cell.Config(Cell.AMOUNT_OF_SYNAPSES,
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

  private static class ToolBarCheckBox extends JCheckBox {
    public ToolBarCheckBox(Action a) {
      super(a);
      this.setFont(new Font(null, 0, 10));
    }
  }

  private static class ToolLabel extends JLabel {
    private ToolLabel() {
      super();
      //this.setFont(new Font(null, Font.BOLD, 10));
    }
  }

  private class ControlPanel extends JPanel implements Observer {
    /*
 Controls
  */
    private Action addPatternAction;
    private Action runAction;
    private Action stepAction;
    private Action stopAction;
    private Action spatialLearningAction;
    private Action temporalLearningAction;
    private Action fullSpeedAction;
    private Action temporalSplitAction;

    final JToolBar toolBar = new JToolBar();
    final Container infoPane = new Container();
    private JLabel pattersStepInfo = new ToolLabel();
    private JLabel cycleInfo = new ToolLabel();
    private JLabel tempInfo = new ToolLabel();


    @Override
    public void update(Observable o, Object arg) {
      enableActions();
      infoPane.setVisible(patterns.size() > 0);
      pattersStepInfo.setText("Size-Step: " + patterns.size() + "-" + process.getCurrentPatternIndex());
      cycleInfo.setText("Cycle: " + process.getCycle());
      tempInfo.setText("Ph:" + (process.temporalPhasePointer == 0 ? 3 : process.temporalPhasePointer));
    }


    private void enableActions() {
      runAction.setEnabled(patterns.size() > 0 && !process.isRunning());
      stepAction.setEnabled(patterns.size() > 0);
      stopAction.setEnabled(patterns.size() > 0 && process.isRunning());
    }


    private void initActions() {

      addPatternAction = new AbstractAction("Add", UIUtils.INSTANCE.createImageIcon(
              "/images/add.png")) {
        @Override public void actionPerformed(ActionEvent e) {
          addPattern();
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
          synchronized (temporalSplitLock) {
            process.step();
          }
          process.sendUpdateNotification();
        }

      };

      stopAction = new AbstractAction("Stop", UIUtils.INSTANCE.createImageIcon(
              "/images/stop.png")) {
        @Override public void actionPerformed(ActionEvent e) {
          process.stop();
        }
      };

      spatialLearningAction = new AbstractAction("Learn Spat") {
        @Override public void actionPerformed(ActionEvent e) {
          layer.setSpatialLearning(!layer.getSpatialLearning());
        }

      };
      spatialLearningAction.putValue(Action.SELECTED_KEY, layer.getSpatialLearning());

      temporalLearningAction = new AbstractAction("Learn Temp") {
        @Override public void actionPerformed(ActionEvent e) {
          temporalPooler.setLearningMode(!temporalPooler.isLearningMode());
        }

      };

      temporalLearningAction.putValue(Action.SELECTED_KEY, temporalPooler.isLearningMode());

      fullSpeedAction = new AbstractAction("Full Speed!") {
        @Override public void actionPerformed(ActionEvent e) {
          process.setFullSpeed(!process.isFullSpeed());
        }

      };

      fullSpeedAction.putValue(Action.SELECTED_KEY, HTMProcess.FULL_SPEED_DEFAULT);

      temporalSplitAction = new AbstractAction("Temp Split") {
        @Override public void actionPerformed(ActionEvent e) {
          process.sendUpdateNotification();
          boolean fullSpeed = process.isFullSpeed();
          process.setFullSpeed(true);
          synchronized (temporalSplitLock) {
            if (process.isTemporalSplit()) {
              while (process.temporalPhasePointer != 0) {
                LOG.debug("Completing temporal split phases on switch: #" + process.temporalPhasePointer);
                process.step();
              }
            }
            process.setTemporalSplit(!process.isTemporalSplit());
            tempInfo.setVisible(process.isTemporalSplit());
          }
          process.sendUpdateNotification();
          process.setFullSpeed(fullSpeed);
        }
      };

      temporalSplitAction.putValue(Action.SELECTED_KEY, HTMProcess.TEMPORAL_SPLIT_DEFAULT);

      enableActions();

    }


    public ControlPanel() {
      initActions();
      infoPane.setPreferredSize(new Dimension(200, infoPane.getPreferredSize().height));
      /*infoPane.setLayout(new GridLayout(0, 3, 1, 1));
      infoPane.add(pattersStepInfo);
      infoPane.add(cycleInfo);
      infoPane.add(tempInfo);*/
      infoPane.setLayout(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridx = 0;
      c.gridy = 0;
      infoPane.add(pattersStepInfo, c);
      c.gridx = 1;
      c.weightx = 1.5;
      infoPane.add(cycleInfo, c);
      c.gridx = 2;
      c.weightx = 0.5;
      infoPane.add(tempInfo, c);
      infoPane.setVisible(patterns.size() > 0);
      toolBar.add(new JButton(addPatternAction));
      toolBar.add(new JButton(runAction));
      toolBar.add(new JButton(stepAction));
      toolBar.add(new JButton(stopAction));
      /**
       toolBar.add(new JComponent() {
       private Container init() {
       this.setLayout(new GridLayout(2, 0, 0, 0));
       add(new ToolBarCheckBox(spatialLearningAction));
       add(new ToolBarCheckBox(temporalLearningAction));
       add(sdrInput);
       return this;
       }
       }.init()); **/
      toolBar.add(new ToolBarCheckBox(spatialLearningAction));
      toolBar.add(new ToolBarCheckBox(temporalLearningAction));
      toolBar.add(new ToolBarCheckBox(fullSpeedAction));
      toolBar.add(new ToolBarCheckBox(temporalSplitAction));
      toolBar.addSeparator();
      toolBar.add(infoPane);
      this.add(toolBar);
      tempInfo.setVisible(false);
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

  public void clearInputSpace() {
    sensoryInputSurface.reset();
  }

  public void addPattern() {
    patterns.add(sensoryInputSurface.getSensoryInputValues());
    layer.performSpatialPooling();
    temporalPooler.execute();
    process.sendUpdateNotification();
    this.repaint();
  }

  public void resetPatterns() {
    patterns.clear();
    sensoryInputSurface.reset();
    process.reset();
  }

  private class HTMProcess extends Observable {
    public final static boolean FULL_SPEED_DEFAULT = false;
    public final static boolean TEMPORAL_SPLIT_DEFAULT = false;
    private volatile int currentPatternIndex = 0;
    private volatile int cycleCounter = 0;
    private ExecutorService es = Executors.newSingleThreadExecutor();
    private ExecutorService esUpdate = Executors.newSingleThreadExecutor();
    private Future<Boolean> processFuture;
    private volatile boolean fullSpeed = FULL_SPEED_DEFAULT;
    /*DEBUG Option - We need this mode to brake point temporal polling between phases to see formation of new segments and segments updates from phase to phase
    */
    private volatile boolean temporalSplit = TEMPORAL_SPLIT_DEFAULT;
    volatile int temporalPhasePointer = 0;

    public boolean isFullSpeed() {
      return fullSpeed;
    }

    public void setFullSpeed(boolean on) {
      fullSpeed = on;
    }

    public boolean isTemporalSplit() {
      return temporalSplit;
    }

    public void setTemporalSplit(boolean temporalSplit) {
      this.temporalSplit = temporalSplit;
    }

    public void sendUpdateNotification() {
      esUpdate.submit(new Callable<Object>() {
        @Override public Object call() throws Exception {
          setChanged();
          notifyObservers();
          return null;
        }
      });
    }

    public boolean step() {
      if (patterns.size() != 0) {
        sensoryInputSurface.setSensoryInputValues(patterns.get(currentPatternIndex));
        try {
          if (!fullSpeed) {
            //Added timeout to avoid accident lock down
            viewsUpdateLatch.await(200, TimeUnit.MILLISECONDS);
          }
        } catch (InterruptedException e) {
          LOG.error("viewsUpdateLatch interrupted", e);
        }
        if (!temporalSplit) {
          LOG.debug("Start step #" + process.getCycle() + ", " + process.currentPatternIndex);
          layer.performSpatialPooling();
          temporalPooler.execute();
        } else {
          switch (temporalPhasePointer) {
            case 0: {
              LOG.debug("Start step #" + process.getCycle() + ", " + process.currentPatternIndex);
              layer.performSpatialPooling();
              temporalPooler.nextTimeStep();
              temporalPooler.phaseOne();
              temporalPhasePointer = 1;
              break;
            }
            case 1:
              temporalPooler.phaseTwo();
              temporalPhasePointer = 2;
              break;
            case 2:
              temporalPooler.phaseThree();
              temporalPhasePointer = 0;
              break;
            default:
              throw new RuntimeException("Invalid Temporal Phase Pointer:" + temporalPhasePointer);
          }
        }
        if (!temporalSplit || temporalPhasePointer == 0) {
          if (currentPatternIndex < patterns.size() - 1) {
            currentPatternIndex++;
          } else {
            cycleCounter++;
            currentPatternIndex = 0;
          }
        }
        if (!fullSpeed) {
          viewsUpdateLatch = new CountDownLatch(2);
        }
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

          while (!processFuture.isCancelled()) {
            synchronized (temporalSplitLock) {
              step();
            }
            sendUpdateNotification();
          }
          return false;
        }
      });
    }

    public void stop() {
      if (processFuture != null) {
        processFuture.cancel(true);
      }
    }

    public boolean isRunning() {
      if (processFuture != null) {
        return !processFuture.isDone();
      } else {
        return false;
      }
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
    private final TemporalPooler.Config temporalPoolerConfig;
    private final Layer.Config regionConfig;
    private final Column.Config columnConfig;
    private final Cell.Config cellConfig;
    private final Synapse.Config proximalSynapseConfig;
    private final Synapse.Config distalSynapseConfig;


    public Config(List<boolean[]> patterns, TemporalPooler.Config temporalPoolerConfig, Layer.Config regionConfig,
                  Column.Config columnConfig,
                  Cell.Config cellConfig,
                  Synapse.ProximalSynapse.Config proximalSynapseConfig,
                  Synapse.DistalSynapse.Config distalSynapseConfig) {
      this.patterns = patterns;
      this.temporalPoolerConfig = temporalPoolerConfig;
      this.regionConfig = regionConfig;
      this.columnConfig = columnConfig;
      this.cellConfig = cellConfig;
      this.proximalSynapseConfig = proximalSynapseConfig;
      this.distalSynapseConfig = distalSynapseConfig;
    }

    public Layer.Config getRegionConfig() {
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


    public TemporalPooler.Config getTemporalPoolerConfig() {
      return temporalPoolerConfig;
    }
  }

}



