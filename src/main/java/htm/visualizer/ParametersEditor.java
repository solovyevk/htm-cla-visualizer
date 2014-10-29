/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;

public class ParametersEditor extends JComponent {
  private final static Color LIGHT_BLUE = new Color(150, 150, 255);
  private static Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(4, 4, 4, 4);
  private static Font LABEL_FONT = UIManager.getFont("JLabel.font");
  private static Color TITLE_COLOR = UIManager.getColor("Slider.foreground");


  public static HTMGraphicInterface.Config showDialog(Component component,
                                                      String title,
                                                      HTMGraphicInterface.Config initialParams) throws HeadlessException {

    final ParametersEditor pane = new ParametersEditor(initialParams);

    ParametersTracker ok = new ParametersTracker(pane);
    JDialog dialog = createDialog(component, title, true, pane, ok, null);

    dialog.show(); // blocks until user brings dialog down...

    return ok.getParameters();
  }

  public static JDialog createDialog(Component c, String title, boolean modal,
                                     ParametersEditor chooserPane, ActionListener okListener,
                                     ActionListener cancelListener) throws HeadlessException {

    Window window = getWindowForComponent(c);
    ParametersEditorDialog dialog;
    if (window instanceof Frame) {
      dialog = new ParametersEditorDialog((Frame)window, title, modal, c, chooserPane,
                                          okListener, cancelListener);
    } else {
      dialog = new ParametersEditorDialog((Dialog)window, title, modal, c, chooserPane,
                                          okListener, cancelListener);
    }
    return dialog;
  }

  static Window getWindowForComponent(Component parentComponent)
          throws HeadlessException {
    if (parentComponent == null) {
      return JOptionPane.getRootFrame();
    }
    if (parentComponent instanceof Frame || parentComponent instanceof Dialog) {
      return (Window)parentComponent;
    }
    return getWindowForComponent(parentComponent.getParent());
  }

  private Parameters.TemporalPoolerParameters temporalPoolerParameters;
  private Parameters.SpatialPoolerParameters spatialPoolerParameters;
  private Parameters.RegionParameters regionParameters;
  private Parameters.ColumnParameters columnParameters;
  private Parameters.CellParameters cellParameters;
  private Parameters.SynapseParameters proximalSynapsesParameters;
  private Parameters.SynapseParameters distalSynapsesParameters;

  public void setParameters(HTMGraphicInterface.Config params) {
    temporalPoolerParameters.setParameters(params.getTemporalPoolerConfig());
    spatialPoolerParameters.setParameters(params.getSpatialPoolerConfig());
    regionParameters.setParameters(params.getRegionConfig());
    columnParameters.setParameters(params.getColumnConfig());
    proximalSynapsesParameters.setParameters(params.getProximalSynapseConfig());
    distalSynapsesParameters.setParameters(params.getDistalSynapseConfig());
    cellParameters.setParameters(params.getCellConfig());
  }

  public HTMGraphicInterface.Config getParameters() {
    return new HTMGraphicInterface.Config(null,
                                          temporalPoolerParameters.getParameters(),
                                          spatialPoolerParameters.getParameters(),
                                          regionParameters.getParameters(),
                                          columnParameters.getParameters(),
                                          cellParameters.getParameters(),
                                          proximalSynapsesParameters.getParameters(),
                                          distalSynapsesParameters.getParameters());
  }

  public ParametersEditor(HTMGraphicInterface.Config params) {
    regionParameters = new Parameters.RegionParameters(params.getRegionConfig());
    columnParameters = new Parameters.ColumnParameters(params.getColumnConfig());
    cellParameters = new Parameters.CellParameters(params.getCellConfig());
    spatialPoolerParameters = new Parameters.SpatialPoolerParameters(params.getSpatialPoolerConfig());
    temporalPoolerParameters = new Parameters.TemporalPoolerParameters(params.getTemporalPoolerConfig());
    proximalSynapsesParameters = new Parameters.SynapseParameters(params.getProximalSynapseConfig());
    distalSynapsesParameters = new Parameters.SynapseParameters(params.getDistalSynapseConfig());

    this.setLayout(new BorderLayout());
    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("General", new JComponent() {
      private Container init() {
        this.setLayout(new BorderLayout());
        this.add(decorateWithBorder(regionParameters, "Region Properties"));
        return this;
      }
    }.init());
    tabs.addTab("Spatial", new JComponent() {
      private Container init() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 3.0;
        this.add(decorateWithBorder(spatialPoolerParameters, "Spatial Pooler"), c);
        c.gridy = 1;
        c.weighty = 1.0;
        this.add(decorateWithBorder(columnParameters, "Column Parameters"), c);
        c.gridy = 2;
        c.weighty = 3.0;
        this.add(decorateWithBorder(proximalSynapsesParameters, "Proximal Synapses"), c);
        return this;
      }
    }.init());
    tabs.addTab("Temporal", new JComponent() {
      private Container init() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 3.0;
        this.add(decorateWithBorder(temporalPoolerParameters, "Temporal Pooler"), c);
        c.gridy = 1;
        c.weighty = 2.0;
        this.add(decorateWithBorder(cellParameters, "Cell Parameters"), c);
        c.gridy = 2;
        c.weighty = 3.0;
        this.add(decorateWithBorder(distalSynapsesParameters, "Distal Synapses"), c);
        return this;
      }
    }.init());
    this.add(tabs);

   /* this.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 0;
    c.weighty = 6.0;
    c.weightx = 1.0;
    this.add(decorateWithBorder(regionParameters,"Region Parameters"), c);
    c.gridy = 1;
    c.weighty = 5.0;
    this.add(decorateWithBorder(columnParameters, "Column Parameters"), c);
    c.gridy = 2;
    c.weighty = 3.0;
    this.add(decorateWithBorder(proximalSynapsesParameters,"Proximal Synapses"), c);
    c.gridy = 3;
    this.add(decorateWithBorder(distalSynapsesParameters, "Distal Synapses"), c); */
  }

  private JComponent decorateWithBorder(JComponent component, String title) {
    component.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(null, title, TitledBorder.CENTER, TitledBorder.TOP, LABEL_FONT,
                                             TITLE_COLOR),
            DEFAULT_BORDER));
    return component;
  }

}

class ParametersEditorDialog extends JDialog {
  private HTMGraphicInterface.Config initialCfg;
  private ParametersEditor chooserPane;
  private JButton cancelButton;

  public ParametersEditorDialog(Dialog owner, String title, boolean modal,
                                Component c, ParametersEditor chooserPane,
                                ActionListener okListener, ActionListener cancelListener)
          throws HeadlessException {
    super(owner, title, modal);
    initParametersChooserDialog(c, chooserPane, okListener, cancelListener);
  }

  public ParametersEditorDialog(Frame owner, String title, boolean modal,
                                Component c, ParametersEditor chooserPane,
                                ActionListener okListener, ActionListener cancelListener)
          throws HeadlessException {
    super(owner, title, modal);
    initParametersChooserDialog(c, chooserPane, okListener, cancelListener);
  }

  protected void initParametersChooserDialog(Component c, ParametersEditor chooserPane,
                                             ActionListener okListener, ActionListener cancelListener) {
    //setResizable(false);

    this.chooserPane = chooserPane;

    String okString = "Ok";
    String cancelString = "Cancel";
    String resetString = "Reset";

    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());
    contentPane.add(chooserPane, BorderLayout.CENTER);

        /*
         * Create Lower button panel
         */
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new FlowLayout(FlowLayout.CENTER));
    JButton okButton = new JButton(okString);
    getRootPane().setDefaultButton(okButton);
    okButton.setActionCommand("OK");
    okButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        hide();
      }
    });
    if (okListener != null) {
      okButton.addActionListener(okListener);
    }
    buttonPane.add(okButton);

    cancelButton = new JButton(cancelString);

    // The following few lines are used to register esc to close the dialog
    Action cancelKeyAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (ActionListener a : ((AbstractButton)e.getSource()).getActionListeners()) {
          a.actionPerformed(e);
        }
      }
    };
    KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke((char)KeyEvent.VK_ESCAPE, false);
    InputMap inputMap = cancelButton.getInputMap(JComponent.
                                                         WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = cancelButton.getActionMap();
    if (inputMap != null && actionMap != null) {
      inputMap.put(cancelKeyStroke, "cancel");
      actionMap.put("cancel", cancelKeyAction);
    }
    // end esc handling

    cancelButton.setActionCommand("cancel");
    cancelButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        hide();
      }
    });
    if (cancelListener != null) {
      cancelButton.addActionListener(cancelListener);
    }
    buttonPane.add(cancelButton);

    JButton resetButton = new JButton(resetString);
    resetButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        reset();
      }
    });
    buttonPane.add(resetButton);
    contentPane.add(buttonPane, BorderLayout.SOUTH);

    if (JDialog.isDefaultLookAndFeelDecorated()) {
      boolean supportsWindowDecorations =
              UIManager.getLookAndFeel().getSupportsWindowDecorations();
      if (supportsWindowDecorations) {
        getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
      }
    }
    applyComponentOrientation(((c == null) ? getRootPane() : c).getComponentOrientation());
    pack();
    setLocationRelativeTo(c);

    this.addWindowListener(new Closer());
    this.addComponentListener(new DisposeOnClose());
  }

  @Override
  public void show() {
    initialCfg = chooserPane.getParameters();
    super.show();
  }

  public void reset() {
    chooserPane.setParameters(initialCfg);
  }

  class Closer extends WindowAdapter implements Serializable {
    @Override
    public void windowClosing(WindowEvent e) {
      cancelButton.doClick(0);
      Window w = e.getWindow();
      w.hide();
    }
  }

  static class DisposeOnClose extends ComponentAdapter implements Serializable {
    @Override
    public void componentHidden(ComponentEvent e) {
      Window w = (Window)e.getComponent();
      w.dispose();
    }
  }
}

class ParametersTracker implements ActionListener, Serializable {
  ParametersEditor chooser;
  HTMGraphicInterface.Config cfg;

  public ParametersTracker(ParametersEditor c) {
    chooser = c;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    cfg = chooser.getParameters();
  }

  public HTMGraphicInterface.Config getParameters() {
    return cfg;
  }
}

