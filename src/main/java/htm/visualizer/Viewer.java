/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer;

import javax.swing.*;

public class Viewer extends JFrame {

  public Viewer() {
    initUI();
  }

  private void initUI() {
    setTitle("CLA Visualiser");
    HTMGraphicInterface htmInterface = new HTMGraphicInterface();
    add(htmInterface);
    setJMenuBar(htmInterface.createMenuBar());
    setSize(1100, 800);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
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
