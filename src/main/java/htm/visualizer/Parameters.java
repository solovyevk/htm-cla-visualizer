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
import htm.model.Synapse;
import htm.utils.MathUtils;
import htm.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Hashtable;

public class Parameters {

  static class FixedWidthLabel extends JLabel {
    @Override public Dimension getPreferredSize() {
      return new Dimension(135, super.getPreferredSize().height);
    }

    FixedWidthLabel(String text) {
      super(text);
    }
  }

  static abstract class NumberParameter<E extends Number> extends Container implements ChangeListener {
    protected static final int SLIDER_SCALE = 100;
    protected final int sliderScale;

    protected JSpinner spinnerField;
    protected JSlider sliderField;

    NumberParameter(E minValue, E maxValue, E value) {
      this(minValue, maxValue, value, SLIDER_SCALE);
    }

    NumberParameter(E minValue, E maxValue, E value, int sliderScale) {
      this.sliderScale = sliderScale;
      setLayout(new GridBagLayout());
      initFields(minValue, maxValue, value);
      spinnerField.setPreferredSize(new Dimension(60, 30));
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weighty = 1.0;
      c.weightx = 3;
      this.add(sliderField, c);
      c.weightx = 1;
      this.add(spinnerField, c);
      spinnerField.addChangeListener(this);
      sliderField.addChangeListener(this);
    }

    public void setValue(E value) {
      if (sliderField.getValue() != value.intValue()) {
        setSliderValue(value);
      }
      if (spinnerField.getValue() != value) {
        spinnerField.setValue(value);
      }
    }

    public E getValue() {
      return (E)spinnerField.getValue();
    }

    @Override public void stateChanged(ChangeEvent e) {
      if (e.getSource() instanceof JSlider) {
        setValue(getSliderValue());
      } else if (e.getSource() instanceof JSpinner) {
        setValue((E)spinnerField.getValue());
      }
    }

    abstract protected void initFields(E minValue, E maxValue, E value);

    abstract protected E getSliderValue();

    abstract protected void setSliderValue(E value);
  }

  static class IntegerParameter extends NumberParameter<Integer> {

    IntegerParameter(Integer minValue, Integer maxValue, Integer value) {
      super(minValue, maxValue, value);
    }

    @Override protected void initFields(Integer minValue, Integer maxValue, Integer value) {
      spinnerField = new JSpinner(
              new SpinnerNumberModel(value.intValue(), minValue.intValue(), maxValue.intValue(), 1));
      sliderField = new JSlider(JSlider.HORIZONTAL, minValue, maxValue, value);
      sliderField.setPaintLabels(true);
      Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
      labelTable.put(minValue, new JLabel(minValue + ""));
      labelTable.put((maxValue - Math.abs(minValue)) / 2, new JLabel((maxValue - Math.abs(minValue)) / 2 + ""));
      labelTable.put(maxValue, new JLabel(maxValue + ""));
      sliderField.setLabelTable(labelTable);
    }

    @Override protected Integer getSliderValue() {
      return sliderField.getValue();
    }

    @Override protected void setSliderValue(Integer value) {
      sliderField.setValue(value);
    }
  }

  static class DoubleParameter extends NumberParameter<Double> {

    DoubleParameter(Double minValue, Double maxValue, Double value, int sliderScale) {
      super(minValue, maxValue, value, sliderScale);
    }

    DoubleParameter(Double minValue, Double maxValue, Double value) {
      super(minValue, maxValue, value);
    }

    @Override public void setValue(Double value) {
      Double valueConverted = MathUtils.round(value, 3);
      Double sliderValueConverted = MathUtils.round(1.0 * sliderField.getValue() / sliderScale, 3);
      Double spinnerValueConverted = MathUtils.round((Double)spinnerField.getValue(), 3);
      if (sliderValueConverted.doubleValue() != value) {
        setSliderValue(valueConverted);
      }
      if (spinnerValueConverted.doubleValue() != value) {
        spinnerField.setValue(valueConverted);
      }
    }

    @Override protected void initFields(Double minValue, Double maxValue, Double value) {
      int sliderMin = (int)(minValue * sliderScale),
              sliderMax = (int)(maxValue * sliderScale),
              sliderValue = (int)(value * sliderScale);
      spinnerField = new JSpinner(
              new SpinnerNumberModel(value, minValue, maxValue, new Double((maxValue - minValue) / sliderScale)));
      sliderField = new JSlider(JSlider.HORIZONTAL, sliderMin, sliderMax,
                                sliderValue);
      sliderField.setPaintLabels(true);
      Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
      labelTable.put(sliderMin, new JLabel(minValue + ""));
      labelTable.put((sliderMax - Math.abs(sliderMin)) / 2, new JLabel((maxValue - Math.abs(minValue)) / 2 + ""));
      labelTable.put(sliderMax, new JLabel(maxValue + ""));
      sliderField.setLabelTable(labelTable);
    }

    @Override protected Double getSliderValue() {
      return 1.0 * sliderField.getValue() / sliderScale;
    }

    @Override protected void setSliderValue(Double value) {
      sliderField.setValue((int)(value * sliderScale));
    }

  }

  static class RegionParameters extends JPanel {
    private final Region.Config cfg;
    private Parameters.IntegerParameter regionWidthParam;
    private Parameters.IntegerParameter regionHeightParam;
    private Parameters.IntegerParameter inputSpaceWidthParam;
    private Parameters.IntegerParameter inputSpaceHeightParam;
    private DoubleParameter learningRadiusParam;
    private DoubleParameter inputRadiusParam;
    private Parameters.IntegerParameter cellsInColumnParam;
    private JCheckBox skipSpatialCb;

    RegionParameters(Region.Config cfg) {
      this.cfg = cfg;
      setLayout(new SpringLayout());
      regionWidthParam = new IntegerParameter(1, 50, cfg.getRegionDimension().width);
      regionHeightParam = new IntegerParameter(1, 50, cfg.getRegionDimension().height);
      inputSpaceWidthParam = new IntegerParameter(1, 50, cfg.getSensoryInputDimension().width);
      inputSpaceHeightParam = new IntegerParameter(1, 50, cfg.getSensoryInputDimension().height);
      learningRadiusParam = new DoubleParameter(1.0, 25.0, cfg.getInputRadius(), 200);
      inputRadiusParam = new DoubleParameter(1.0, 25.0, cfg.getInputRadius(), 200);
      cellsInColumnParam = new IntegerParameter(1, 20, cfg.getCellsInColumn());
      skipSpatialCb = new JCheckBox(null, null, cfg.isSkipSpatial());
      JLabel l = new FixedWidthLabel("Region Width");
      this.add(l);
      this.add(regionWidthParam);
      l = new FixedWidthLabel("Region Height");
      this.add(l);
      this.add(regionHeightParam);
      l = new FixedWidthLabel("Input Space Width");
      this.add(l);
      this.add(inputSpaceWidthParam);
      l = new FixedWidthLabel("Input Space Height");
      this.add(l);
      this.add(inputSpaceHeightParam);
      l = new FixedWidthLabel("Input Radius");
      this.add(l);
      this.add(inputRadiusParam);
      l = new FixedWidthLabel("Learning Radius");
      this.add(l);
      this.add(learningRadiusParam);
      l = new FixedWidthLabel("Cells per Column");
      this.add(l);
      this.add(cellsInColumnParam);
      l = new FixedWidthLabel("Skip Spatial");
      this.add(l);
      this.add(skipSpatialCb);
      UIUtils.makeSpringCompactGrid(this,
                                    8, 2, //rows, cols
                                    6, 6,        //initX, initY
                                    6, 6);       //xPad, yPad
    }

    Region.Config getParameters() {
      return new Region.Config(new Dimension(regionWidthParam.getValue(),
                                             regionHeightParam.getValue()),
                               new Dimension(inputSpaceWidthParam.getValue(),
                                             inputSpaceHeightParam.getValue()),
                               inputRadiusParam.getValue(),
                               learningRadiusParam.getValue(),
                               skipSpatialCb.isSelected(),
                               cellsInColumnParam.getValue());
    }

    void setParameters(Region.Config cfg) {
      regionWidthParam.setValue(cfg.getRegionDimension().width);
      regionHeightParam.setValue(cfg.getRegionDimension().height);
      inputSpaceWidthParam.setValue(cfg.getSensoryInputDimension().width);
      inputSpaceHeightParam.setValue(cfg.getSensoryInputDimension().height);
      inputRadiusParam.setValue(cfg.getInputRadius());
      skipSpatialCb.setSelected(cfg.isSkipSpatial());
    }
  }

  static class ColumnParameters extends JPanel {
    private final Column.Config cfg;
    private Parameters.IntegerParameter amountOfProximalSynapsesParam;
    private Parameters.IntegerParameter minOverlapParam;
    private Parameters.IntegerParameter desiredLocalActivityParam;
    private Parameters.DoubleParameter boostRateParam;

    ColumnParameters(Column.Config cfg) {
      this.cfg = cfg;
      setLayout(new SpringLayout());
      amountOfProximalSynapsesParam = new IntegerParameter(2, 60, cfg.getAmountOfProximalSynapses());
      minOverlapParam = new IntegerParameter(1, 10, cfg.getMinOverlap());
      desiredLocalActivityParam = new IntegerParameter(1, 10, cfg.getDesiredLocalActivity());
      boostRateParam = new Parameters.DoubleParameter(0.005, 0.2, cfg.getBoostRate(), 200);
      JLabel l = new FixedWidthLabel("N of Proximal Synapses");
      this.add(l);
      this.add(amountOfProximalSynapsesParam);
      l = new FixedWidthLabel("Min Overlap");
      this.add(l);
      this.add(minOverlapParam);
      l = new FixedWidthLabel("Desired Local Activity");
      this.add(l);
      this.add(desiredLocalActivityParam);
      l = new FixedWidthLabel("Boost Rate");
      this.add(l);
      this.add(boostRateParam);
      UIUtils.makeSpringCompactGrid(this,
                                    4, 2, //rows, cols
                                    6, 6,        //initX, initY
                                    6, 6);       //xPad, yPad
    }

    Column.Config getParameters() {
      return new Column.Config(amountOfProximalSynapsesParam.getValue(),
                               minOverlapParam.getValue(),
                               desiredLocalActivityParam.getValue(),
                               boostRateParam.getValue());
    }

    void setParameters(Column.Config cfg) {
      amountOfProximalSynapsesParam.setValue(cfg.getAmountOfProximalSynapses());
      minOverlapParam.setValue(cfg.getMinOverlap());
      desiredLocalActivityParam.setValue(cfg.getDesiredLocalActivity());
      boostRateParam.setValue(cfg.getBoostRate());
    }
  }

  static class CellParameters extends JPanel {
    private final Cell.Config cfg;
    private Parameters.IntegerParameter newSynapseCountParam;
    private Parameters.IntegerParameter activationThresholdParam;
    private Parameters.IntegerParameter minThresholdParam;
    private Parameters.IntegerParameter amountOfSynapsesParam;
    private Parameters.IntegerParameter timeStepsParam;

    CellParameters(Cell.Config cfg) {
      this.cfg = cfg;
      setLayout(new SpringLayout());
      newSynapseCountParam = new IntegerParameter(1, 10, cfg.getNewSynapseCount());
      activationThresholdParam = new IntegerParameter(0, 15, cfg.getActivationThreshold());
      minThresholdParam = new IntegerParameter(0, 5, cfg.getMinThreshold());
      amountOfSynapsesParam = new IntegerParameter(5, 60, cfg.getAmountOfSynapses());
      timeStepsParam = new IntegerParameter(2, 30, cfg.getTimeSteps());

      JLabel l = new FixedWidthLabel("N of New Synapses");
      this.add(l);
      this.add(newSynapseCountParam);
      l = new FixedWidthLabel("Activation Threshold");
      this.add(l);
      this.add(activationThresholdParam);
      l = new FixedWidthLabel("Minimum Threshold");
      this.add(l);
      this.add(minThresholdParam);
      l = new FixedWidthLabel("Amount of Synapses");
      this.add(l);
      this.add(amountOfSynapsesParam);
      l = new FixedWidthLabel("Time Buffer");
      this.add(l);
      this.add(timeStepsParam);
      UIUtils.makeSpringCompactGrid(this,
                                    5, 2, //rows, cols
                                    6, 6,        //initX, initY
                                    6, 6);       //xPad, yPad
    }

    Cell.Config getParameters() {
      return new Cell.Config(newSynapseCountParam.getValue(),
                             activationThresholdParam.getValue(),
                             minThresholdParam.getValue(),
                             amountOfSynapsesParam.getValue(),
                             timeStepsParam.getValue());
    }

    void setParameters(Cell.Config cfg) {
      newSynapseCountParam.setValue(cfg.getNewSynapseCount());
      activationThresholdParam.setValue(cfg.getActivationThreshold());
      minThresholdParam.setValue(cfg.getMinThreshold());
      amountOfSynapsesParam.setValue(cfg.getAmountOfSynapses());
      timeStepsParam.setValue(cfg.getTimeSteps());
    }
  }


  static class SynapseParameters extends JPanel {
    private final Synapse.Config cfg;
    private Parameters.DoubleParameter connectedPermanenceParam;
    private Parameters.DoubleParameter incPermanenceParam;
    private Parameters.DoubleParameter decPermanenceParam;

    SynapseParameters(Synapse.Config cfg) {
      this.cfg = cfg;
      setLayout(new SpringLayout());
      connectedPermanenceParam = new Parameters.DoubleParameter(0.0, 1.0, cfg.getConnectedPerm());
      incPermanenceParam = new Parameters.DoubleParameter(0.005, 0.2, cfg.getPermanenceInc(), 200);
      decPermanenceParam = new Parameters.DoubleParameter(0.005, 0.2, cfg.getPermanenceDec(), 200);
      JLabel l = new FixedWidthLabel("Conn.Permanence");
      this.add(l);
      this.add(connectedPermanenceParam);
      l = new FixedWidthLabel("Increase By");
      this.add(l);
      this.add(incPermanenceParam);
      l = new FixedWidthLabel("Decrease By");
      this.add(l);
      this.add(decPermanenceParam);
      UIUtils.makeSpringCompactGrid(this,
                                    3, 2, //rows, cols
                                    6, 6,        //initX, initY
                                    6, 6);       //xPad, yPad
    }

    Synapse.Config getParameters() {
      return new Synapse.Config(connectedPermanenceParam.getValue(), incPermanenceParam.getValue(),
                                decPermanenceParam.getValue());
    }

    void setParameters(Synapse.Config cfg) {
      connectedPermanenceParam.setValue(cfg.getConnectedPerm());
      incPermanenceParam.setValue(cfg.getPermanenceInc());
      decPermanenceParam.setValue(cfg.getPermanenceDec());
    }


  }


}
