/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer;

import htm.model.Layer;
import htm.utils.UIUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Viewer extends JFrame {
  private static final Log LOG = LogFactory.getLog(UIUtils.class);

  private java.util.List<boolean[]> patterns = new ArrayList<boolean[]>();

  private HTMGraphicInterface htmInterface;
  /*
 Menu Actions
 */
  private Action saveToFileAction;
  private Action loadFromFileAction;
  private Action editParametersAction;
  private Action skipSpatialPoolingAction;
  private Action restartAction;
  private Action htmResetAction;

  private Action addInputAction;
  private Action removePatternsAction;
  private Action cleanInputSpaceAction;


  JCheckBoxMenuItem skipSpatialPoolMenuItem;

  private JFileChooser fc = createFileChooser();

  {
    final Viewer win = this;
    saveToFileAction = new AbstractAction("Save to File", UIUtils.INSTANCE.createImageIcon("/images/disk.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        LOG.debug("Save patterns to File");
        try {
          int returnVal = fc.showSaveDialog(win);
          if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            Serializer.INSTANCE.saveHTMParameters(file, htmInterface.getParameters());
          }
        } catch (Exception ex) {
          LOG.error("Error saving HTM parameters", ex);
          ExceptionHandler.showErrorDialog(ex);
        }
      }
    };

    loadFromFileAction = new AbstractAction("Load from File", UIUtils.INSTANCE.createImageIcon(
            "/images/book_open.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        LOG.debug("Load From File");
        HTMGraphicInterface.Config oldCfg = htmInterface.getParameters();
        try {
          int returnVal = fc.showOpenDialog(win);
          if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            HTMGraphicInterface.Config newCfg = Serializer.INSTANCE.loadHTMParameters(file);
            win.reloadHTMInterface(newCfg);
          }
        } catch (Exception ex) {
          LOG.error("Error loading HTM parameters", ex);
          ExceptionHandler.showErrorDialog(ex);
          win.reloadHTMInterface(oldCfg);
        }
      }
    };

    editParametersAction = new AbstractAction("Edit Parameters", UIUtils.INSTANCE.createImageIcon(
            "/images/cog_edit.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        HTMGraphicInterface.Config oldCfg = htmInterface.getParameters();
        HTMGraphicInterface.Config modCfg = ParametersEditor.showDialog(
                win,
                "Edit Parameters",
                htmInterface.getParameters());
        if (modCfg != null) {
          Layer.Config modRegionCfg = modCfg.getRegionConfig();
          HTMGraphicInterface.Config newCfg = new HTMGraphicInterface.Config(
                  oldCfg.getPatterns(),
                  modCfg.getTemporalPoolerConfig(),
                  modCfg.getSpatialPoolerConfig(),
                  new Layer.Config(
                          modRegionCfg.getRegionDimension(),
                          modRegionCfg.getSensoryInputDimension(),
                          modRegionCfg.getInputRadius(), modRegionCfg.getLearningRadius(), modRegionCfg.isSkipSpatial(),
                          modRegionCfg.getCellsInColumn()),
                  modCfg.getColumnConfig(),
                  modCfg.getCellConfig(),
                  modCfg.getProximalSynapseConfig(),
                  modCfg.getDistalSynapseConfig());
          try {
            win.reloadHTMInterface(newCfg);
          } catch (Exception ex) {
            LOG.error("Error editing HTM parameters", ex);
            ExceptionHandler.showErrorDialog(ex);
            win.reloadHTMInterface(oldCfg);
          }
        }
      }
    };

    skipSpatialPoolingAction = new AbstractAction("Skip Spatial Pooling", UIUtils.INSTANCE.createImageIcon(
            "/images/bullet_go.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        boolean checked = ((JCheckBoxMenuItem)e.getSource()).getState();
        LOG.debug("Skip Spatial Pooling is:" + checked);
        HTMGraphicInterface.Config oldCfg = htmInterface.getParameters();
        Layer.Config oldRegionCfg = oldCfg.getRegionConfig();
        HTMGraphicInterface.Config newCfg = new HTMGraphicInterface.Config(oldCfg.getPatterns(),
                                                                           oldCfg.getTemporalPoolerConfig(),
                                                                           oldCfg.getSpatialPoolerConfig(),
                                                                           new Layer.Config(
                                                                                   oldRegionCfg.getRegionDimension(),
                                                                                   oldRegionCfg.getSensoryInputDimension(),
                                                                                   oldRegionCfg.getInputRadius(),
                                                                                   oldRegionCfg.getLearningRadius(),
                                                                                   checked,
                                                                                   oldRegionCfg.getCellsInColumn()),
                                                                           oldCfg.getColumnConfig(),
                                                                           oldCfg.getCellConfig(),
                                                                           oldCfg.getProximalSynapseConfig(),
                                                                           oldCfg.getDistalSynapseConfig());
        try {
          win.reloadHTMInterface(newCfg);
        } catch (Exception ex) {
          ExceptionHandler.showErrorDialog(ex);
          win.reloadHTMInterface(oldCfg);
        }
      }
    };

    restartAction = new AbstractAction("Restart", UIUtils.INSTANCE.createImageIcon(
            "/images/arrow_rotate_clockwise.png")) {

      @Override public void actionPerformed(ActionEvent e) {
        HTMGraphicInterface.Config cfg;
        InputStream in = getClass().getResourceAsStream("/config.xml");
        try {
          cfg = Serializer.INSTANCE.loadHTMParameters(in);
          win.reloadHTMInterface(cfg);
        } catch (Exception ex) {
          LOG.error("Error loading HTM parameters from config.xml resource", ex);
        } finally {
          try {
            if (in != null) in.close();
          } catch (IOException ex) {
            LOG.error("Can't close config.xml resource stream");
          }
        }
      }
    };

    htmResetAction = new AbstractAction("Clear HTM State", UIUtils.INSTANCE.createImageIcon(
            "/images/eraser.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        LOG.debug("Reset HTM State");
        HTMGraphicInterface.Config cfg = htmInterface.getParameters();
        win.reloadHTMInterface(cfg);
      }
    };

    cleanInputSpaceAction = new AbstractAction("Clean Input Space", UIUtils.INSTANCE.createImageIcon(
            "/images/cleanup.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        htmInterface.clearInputSpace();
      }
    };

    addInputAction = new AbstractAction("Add Input", UIUtils.INSTANCE.createImageIcon(
            "/images/add.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        htmInterface.addPattern();
      }

    };

    removePatternsAction = new AbstractAction("Remove Patterns", UIUtils.INSTANCE.createImageIcon(
            "/images/delete.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        htmInterface.resetPatterns();
      }
    };

  }

  private void reloadHTMInterface(HTMGraphicInterface.Config newCfg) {
    this.remove(htmInterface);
    htmInterface = new HTMGraphicInterface(newCfg);
    this.add(htmInterface);
    skipSpatialPoolMenuItem.setState(htmInterface.getLayer().isSkipSpatial());
    //for java 1.6
    this.invalidate();
    this.validate();
    this.repaint();
  }

  public Viewer() {
    initUI();
  }

  private void initUI() {
    setTitle("CLA Visualizer");
    HTMGraphicInterface.Config cfg;
    InputStream in = getClass().getResourceAsStream("/config.xml");
    try {
      cfg = Serializer.INSTANCE.loadHTMParameters(in);
      htmInterface = new HTMGraphicInterface(cfg);
    } catch (Exception e) {
      LOG.error("Error loading HTM parameters from config.xml resource", e);
      htmInterface = new HTMGraphicInterface();
    } finally {
      try {
        if (in != null) in.close();
      } catch (IOException ex) {
        LOG.error("Can't close config.xml resource stream");
      }
    }
    add(htmInterface);
    setJMenuBar(createMenuBar());
    //sync skipSP state
    skipSpatialPoolMenuItem.setState(htmInterface.getLayer().isSkipSpatial());
    setSize(1300, 880);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
  }

  private JFileChooser createFileChooser() {
    JFileChooser result = new JFileChooser();
    result.setFileFilter(new FileNameExtensionFilter("XML Files", "xml"));
    return result;
  }

  private JMenuBar createMenuBar() {
    JMenuBar menuBar;
    JMenu menu;
    JMenuItem menuItem;
    menuBar = new JMenuBar();
    menu = new JMenu("File");
    menu.setMnemonic(KeyEvent.VK_F);
    menu.getAccessibleContext().setAccessibleDescription(
            "File related operations");
    menuBar.add(menu);

    menuItem = new JMenuItem(saveToFileAction);
    menuItem.setMnemonic(KeyEvent.VK_S);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_1, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Save Patterns & Settings to File");

    menu.add(menuItem);
    menu.addSeparator();
    menuItem = new JMenuItem(loadFromFileAction);
    menuItem.setMnemonic(KeyEvent.VK_L);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_2, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Load Patterns & Settings from File");
    menu.add(menuItem);
    menu = new JMenu("Edit");
    menu.setMnemonic(KeyEvent.VK_E);
    menu.getAccessibleContext().setAccessibleDescription(
            "Edit CLA options");
    menuBar.add(menu);
    menuItem = new JMenuItem(editParametersAction);
    menuItem.setMnemonic(KeyEvent.VK_M);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_3, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Modify CLA parameters");
    menu.add(menuItem);
    menu.addSeparator();

    menuItem = new JMenuItem(restartAction);
    menuItem.setMnemonic(KeyEvent.VK_R);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_4, ActionEvent.ALT_MASK));
    menu.add(menuItem);
    menuItem = new JMenuItem(htmResetAction);
    menuItem.setMnemonic(KeyEvent.VK_H);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_5, ActionEvent.ALT_MASK));
    menu.add(menuItem);
    menu.addSeparator();
    skipSpatialPoolMenuItem = new JCheckBoxMenuItem(skipSpatialPoolingAction);
    skipSpatialPoolMenuItem.setMnemonic(KeyEvent.VK_S);
    skipSpatialPoolMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_6, ActionEvent.ALT_MASK));
    skipSpatialPoolMenuItem.getAccessibleContext().setAccessibleDescription(
            "Skip Spatial Pooling, Connect Input to Temporal Pooling directly");
    menu.add(skipSpatialPoolMenuItem);

    menu = new JMenu("Input");
    menu.setMnemonic(KeyEvent.VK_I);
    menu.getAccessibleContext().setAccessibleDescription(
            "Work with Patterns");
    menuBar.add(menu);
    menuItem = new JMenuItem(addInputAction);
    menuItem.setMnemonic(KeyEvent.VK_A);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_7, ActionEvent.ALT_MASK));
    menu.add(menuItem);
    menuItem = new JMenuItem(cleanInputSpaceAction);
    menuItem.setMnemonic(KeyEvent.VK_C);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_8, ActionEvent.ALT_MASK));
    menu.add(menuItem);
    menuItem = new JMenuItem(removePatternsAction);
    menuItem.setMnemonic(KeyEvent.VK_R);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_9, ActionEvent.ALT_MASK));
    menu.add(menuItem);

    return menuBar;
  }

  public static Frame findActiveFrame() {
    Frame[] frames = JFrame.getFrames();
    for (int i = 0; i < frames.length; i++) {
      Frame frame = frames[i];
      if (frame.isVisible()) {
        return frame;
      }
    }
    return null;
  }

  private static class ExceptionHandler
          implements Thread.UncaughtExceptionHandler {
    private static int SHOW_LINES = 10;

    public static void showErrorDialog(Throwable thrown) {
      ;
      StackTraceElement[] stackTraceElements = thrown.getStackTrace();
      int counter = 0;
      StringBuffer stackTraceBuffer = new StringBuffer();
      for (int i = 0; i < stackTraceElements.length; i++) {
        StackTraceElement stackTraceElement = stackTraceElements[i];
        stackTraceBuffer.append(stackTraceElement.toString()).append("\n");
        if (counter >= SHOW_LINES) {
          break;
        }
        counter++;
      }
      if (stackTraceElements.length > SHOW_LINES) {
        stackTraceBuffer.append("and ").append(stackTraceElements.length - SHOW_LINES).append(" more ...");
      }

      final String errorStackTrace = stackTraceBuffer.toString();
      final String errorMessage = thrown.getMessage();
      Object[] options = {"Close", "Restart"};
      int result = JOptionPane.showOptionDialog(findActiveFrame(),
                                                errorMessage + "\n\n" + errorStackTrace,
                                                "Exception Occurred",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.ERROR_MESSAGE,
                                                null,
                                                options,
                                                options[0]);
      if (result == JOptionPane.NO_OPTION) {
        LOG.debug("Try to restore UI");
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            Frame f = findActiveFrame();
            if (f != null && f instanceof Viewer) {
              Viewer viewer = (Viewer)f;
              HTMGraphicInterface.Config cfg;
              InputStream in = getClass().getResourceAsStream("/config.xml");
              try {
                cfg = Serializer.INSTANCE.loadHTMParameters(in);
                viewer.reloadHTMInterface(cfg);
              } catch (Exception e) {
                LOG.error("Error loading HTM parameters from config.xml resource", e);
              } finally {
                try {
                  if (in != null) in.close();
                } catch (IOException ex) {
                  LOG.error("Can't close config.xml resource stream");
                }
              }
            }
          }
        });
      }
    }

    public void handle(Throwable thrown) {
      // for EDT exceptions
      handleException(Thread.currentThread().getName(), thrown);
    }

    public void uncaughtException(Thread thread, Throwable thrown) {
      // for other uncaught exceptions
      handleException(thread.getName(), thrown);
    }

    protected void handleException(String tname, Throwable thrown) {
      LOG.error("Exception on " + tname);
      LOG.error("Handling uncaught exceptions", thrown);
      showErrorDialog(thrown);
    }
  }


  public static void main(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
    System.setProperty("sun.awt.exception.handler",
                       ExceptionHandler.class.getName());
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final Viewer sk = new Viewer();
        sk.setVisible(true);
      }
    });
  }
}





