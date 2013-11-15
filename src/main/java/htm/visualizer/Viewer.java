/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer;

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
            Serializer.INSTANCE.saveHTMParameters(file, htmInterface);
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
            win.remove(htmInterface);
            htmInterface = new HTMGraphicInterface(newCfg);
            win.add(htmInterface);
            //for java 1.6
            win.invalidate();
            win.validate();
            win.repaint();
          }
        } catch (Exception ex) {
          LOG.error("Error loading HTM parameters", ex);
        }
      }
    };
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
    setSize(1100, 800);
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
    menuBar.add(menu);
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
