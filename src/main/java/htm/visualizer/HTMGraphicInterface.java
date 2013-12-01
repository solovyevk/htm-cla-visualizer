package htm.visualizer;

import htm.model.Cell;
import htm.model.Column;
import htm.model.Region;
import htm.model.Synapse;
import htm.utils.UIUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

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


  /*
  Default HTM Column Parameters
   */
  private static final int CELLS_PER_COLUMN = 3;
  private static final int DESIRED_LOCAL_ACTIVITY = 1;
  private static final int MINIMAL_OVERLAP = 2;
  private static final int AMOUNT_OF_SYNAPSES = 30;
  private static final double BOOST_RATE = 0.01;

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


  private static Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(0, 4, 0, 4);
  private static Border LIGHT_GRAY_BORDER = BorderFactory.createLineBorder(Color.lightGray);

  static {
    Column.BOOST_RATE = BOOST_RATE;
  }

  private java.util.List<boolean[]> patterns = new ArrayList<boolean[]>();


  private HTMProcess process;


  private Region region;
  private final JComponent slicedView;
  private final ControlPanel control;
  private final SensoryInputSurface sensoryInputSurface;
  private final ColumnSDRSurface sdrInput;
  private SpatialInfo spatialInfo;


  public HTMGraphicInterface() {
    this(new Config(null, new Region.Config(new Dimension(HORIZONTAL_COLUMN_NUMBER, VERTICAL_COLUMN_NUMBER),
                                            new Dimension(SENSORY_INPUT_WIDTH, SENSORY_INPUT_HEIGHT), INPUT_RADIUS,
                                            false),
                    new Column.Config(CELLS_PER_COLUMN, AMOUNT_OF_SYNAPSES,
                                      MINIMAL_OVERLAP, DESIRED_LOCAL_ACTIVITY, BOOST_RATE),
                    new Synapse.Config(PROXIMAL_SYNAPSE_CONNECTED_PERMANENCE, PROXIMAL_SYNAPSE_PERMANENCE_INCREASE,
                                       PROXIMAL_SYNAPSE_PERMANENCE_DECREASE),
                    new Synapse.Config(DISTAL_SYNAPSE_CONNECTED_PERMANENCE, DISTAL_SYNAPSE_PERMANENCE_INCREASE,
                                       DISTAL_SYNAPSE_PERMANENCE_DECREASE)));
  }

  public HTMGraphicInterface(Config cfg) {
    super(new BorderLayout(0, 0));
    //Set static attributes for HTM Model classes
    Column.updateFromConfig(cfg.getColumnConfig());
    Synapse.ProximalSynapse.updateFromConfig(cfg.getProximalSynapseConfig());
    Synapse.DistalSynapse.updateFromConfig(cfg.getDistalSynapseConfig());
    //Initialize region and all related UI
    this.region = new Region(cfg.getRegionConfig());
    this.sensoryInputSurface = new SensoryInputSurface(region.getInputSpace());
    this.sdrInput = new ColumnSDRSurface(region);
    this.slicedView = new HTMRegionSlicedView();
    this.control = new ControlPanel();
    if (!region.isSkipSpatial()) {
      this.spatialInfo = new SpatialInfo();
    }
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
        win.repaint();
      }
    });
  }

  private void initListeners() {
    //select column to view details
    if (!region.isSkipSpatial()) {
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
            int inputIndex = (Integer)spatialInfo.getNeighborColumnsTable().getModel().getValueAt(rowColumnModelInx, 6);
            sdrInput.setSelectedColumn(inputIndex);
          }
        }
      });
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
            c.weighty = 0.45;
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

  Region getRegion() {
    return region;
  }


  Config getParameters() {
    return new Config(patterns, new Region.Config(region.getDimension(), region.getInputSpaceDimension(),
                                                  region.getInputRadius(), region.isSkipSpatial()),
                      new Column.Config(Column.CELLS_PER_COLUMN,
                                        Column.AMOUNT_OF_PROXIMAL_SYNAPSES,
                                        Column.MIN_OVERLAP,
                                        Column.DESIRED_LOCAL_ACTIVITY, Column.BOOST_RATE),
                      new Synapse.Config(Synapse.ProximalSynapse.CONNECTED_PERMANENCE,
                                         Synapse.ProximalSynapse.PERMANENCE_INCREASE,
                                         Synapse.ProximalSynapse.PERMANENCE_DECREASE
                      ),
                      new Synapse.Config(Synapse.DistalSynapse.CONNECTED_PERMANENCE,
                                         Synapse.DistalSynapse.PERMANENCE_INCREASE,
                                         Synapse.DistalSynapse.PERMANENCE_DECREASE
                      ));
  }


  private static class SelectedCellsAndDetails extends JPanel {
    public SelectedCellsAndDetails(SpatialInfo spatialInfo) {
      super(new BorderLayout());
      setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Selected/Active Column & Details"),
              DEFAULT_BORDER));
      final SensoryInputSurface top = new SensoryInputSurface(5, 3);
      top.setBorder(LIGHT_GRAY_BORDER);
      JComponent test = new JPanel();
      test.setBackground(Color.WHITE);
      final JTabbedPane bottom = new JTabbedPane();
      if (spatialInfo != null) {
        bottom.addTab("Spatial Info", spatialInfo);
      }
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

    private void enableActions(){
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
        enableActions();

      }


    public ControlPanel() {
      initActions();
      infoPane.setPreferredSize(new Dimension(200, infoPane.getPreferredSize().height));
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


    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    public Dimension getPreferredSize() {
      return new Dimension(400,
                           40);
    }

   public Dimension getMaximumSize() {
      return getPreferredSize();
    }


  }


  private class HTMRegionSlicedView extends JPanel {
    public HTMRegionSlicedView() {
      super(new GridLayout(0, 1));
      setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Region Slices"),
              DEFAULT_BORDER));
      Column[] columns = region.getColumns();
      for (int i = 0; i < Column.CELLS_PER_COLUMN; i++) {
        Cell[] layer = new Cell[columns.length];
        for (int j = 0; j < columns.length; j++) {
          layer[j] = columns[j].getCellByIndex(i);
        }
        final BaseSurface cellLayer = new ColumnCellsByIndexSurface(region.getDimension().width,
                                                                    region.getDimension().height,
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

    @Override
    public Dimension getPreferredSize() {
      int prefWidth = super.getPreferredSize().width;
      /*LOG.debug("HTMRegionSlicedView width:" + getSize().width);
      double cof = getSize().width != 0 ? 1.0 * getSize().width/300 : 1;
      LOG.debug("Coef:" +cof);
      return new Dimension(prefWidth,
                           (int)(270 * Column.CELLS_PER_COLUMN * cof));  */
      return new Dimension(prefWidth, 270 * Column.CELLS_PER_COLUMN);
    }

  }


  /*Control Methods*/
  private void addPattern() {
    patterns.add(sensoryInputSurface.getSensoryInputValues());
    region.performSpatialPooling();
    process.sendUpdateNotification();
    this.repaint();
  }

  private void resetPatterns() {
    patterns.clear();
    sensoryInputSurface.reset();
    process.reset();
  }

  private class HTMProcess extends Observable {
    private boolean running = false;
    private int currentPatternIndex = 0;
    private int cycleCounter = 0;

    public void sendUpdateNotification() {
      setChanged();
      notifyObservers();
    }


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
          cycleCounter++;
          currentPatternIndex = 0;
        }
        sendUpdateNotification();
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

    public int getCycle() {
      return cycleCounter;
    }

    public void reset() {
      running = false;
      currentPatternIndex = 0;
      cycleCounter = 0;
      sendUpdateNotification();
    }
  }

  public static class Config {
    private final java.util.List<boolean[]> patterns;
    private final Region.Config regionConfig;
    private final Column.Config columnConfig;
    private final Synapse.Config proximalSynapseConfig;
    private final Synapse.Config distalSynapseConfig;


    public Config(List<boolean[]> patterns, Region.Config regionConfig, Column.Config columnConfig,
                  Synapse.ProximalSynapse.Config proximalSynapseConfig,
                  Synapse.DistalSynapse.Config distalSynapseConfig) {
      this.patterns = patterns;
      this.regionConfig = regionConfig;
      this.columnConfig = columnConfig;
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
  }

}



