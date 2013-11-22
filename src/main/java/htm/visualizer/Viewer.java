/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer;

import htm.model.Region;
import htm.utils.UIUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
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
        }
      }
    };

    loadFromFileAction = new AbstractAction("Load from File", UIUtils.INSTANCE.createImageIcon(
            "/images/book_open.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        LOG.debug("Load From File");
        try {
          int returnVal = fc.showOpenDialog(win);
          if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            HTMGraphicInterface.Config newCfg = Serializer.INSTANCE.loadHTMParameters(file);
            win.reloadHTMInterface(newCfg);
          }
        } catch (Exception ex) {
          LOG.error("Error loading HTM parameters", ex);
        }
      }
    };

    editParametersAction = new AbstractAction("Edit Parameters", UIUtils.INSTANCE.createImageIcon(
            "/images/cog_edit.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        HTMGraphicInterface.Config oldCfg = htmInterface.getParameters();
        HTMGraphicInterface.Config modCfg = ParametersChooser.showDialog(
                win,
                "Edit Parameters",
                htmInterface.getParameters());
        Region.Config modRegionCfg = modCfg.getRegionConfig();
        HTMGraphicInterface.Config newCfg = new HTMGraphicInterface.Config(oldCfg.getPatterns(), new Region.Config(
                modRegionCfg.getRegionDimension(),
                modRegionCfg.getSensoryInputDimension(),
                modRegionCfg.getInputRadius(), modRegionCfg.isSkipSpatial()), modCfg.getColumnConfig(),
                                                                           modCfg.getProximalSynapseConfig(),
                                                                           modCfg.getDistalSynapseConfig());
        win.reloadHTMInterface(newCfg);
      }
    };

    skipSpatialPoolingAction = new AbstractAction("Skip Spatial Pooling", UIUtils.INSTANCE.createImageIcon(
            "/images/bullet_go.png")) {
      @Override public void actionPerformed(ActionEvent e) {
        boolean checked = ((JCheckBoxMenuItem)e.getSource()).getState();
        LOG.debug("Skip Spatial Pooling is:" + checked);
        HTMGraphicInterface.Config oldCfg = htmInterface.getParameters();
        Region.Config oldRegionCfg = oldCfg.getRegionConfig();
        HTMGraphicInterface.Config newCfg = new HTMGraphicInterface.Config(oldCfg.getPatterns(), new Region.Config(
                oldRegionCfg.getRegionDimension(),
                oldRegionCfg.getSensoryInputDimension(),
                oldRegionCfg.getInputRadius(), checked), oldCfg.getColumnConfig(), oldCfg.getProximalSynapseConfig(),
                                                                           oldCfg.getDistalSynapseConfig());
        win.reloadHTMInterface(newCfg);
      }
    };

  }

  private void reloadHTMInterface(HTMGraphicInterface.Config newCfg) {
    this.remove(htmInterface);
    htmInterface = new HTMGraphicInterface(newCfg);
    this.add(htmInterface);
    skipSpatialPoolMenuItem.setState(htmInterface.getRegion().isSkipSpatial());
    //for java 1.6
    this.invalidate();
    this.validate();
    this.repaint();
  }

  public Viewer() {
    initUI();
  }

  private void initUI() {
    setTitle("CLA Visualiser");
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
    skipSpatialPoolMenuItem.setState(htmInterface.getRegion().isSkipSpatial());
    setSize(1200, 880);
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
    skipSpatialPoolMenuItem = new JCheckBoxMenuItem(skipSpatialPoolingAction);
    skipSpatialPoolMenuItem.setMnemonic(KeyEvent.VK_S);
    skipSpatialPoolMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_4, ActionEvent.ALT_MASK));
    skipSpatialPoolMenuItem.getAccessibleContext().setAccessibleDescription(
            "Skip Spatial Pooling, Connect Input to Temporal Pooling directly");
    menu.add(skipSpatialPoolMenuItem);
    return menuBar;
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {

      public void run() {
        Viewer sk = new Viewer();
        sk.setVisible(true);
      }
    });
  }
}
