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
import htm.model.Layer;
import htm.model.Synapse;
import htm.model.algorithms.spatial.SpatialPooler;
import htm.model.algorithms.temporal.TemporalPooler;
import htm.utils.MathUtils;
import htm.utils.UIUtils;

import java.awt.*;
import java.util.Hashtable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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


        @SuppressWarnings("unchecked") public E getValue() {
            return (E)spinnerField.getValue();
        }

        @SuppressWarnings("unchecked") @Override public void stateChanged(ChangeEvent e) {
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
        private final Parameters.IntegerParameter regionWidthParam;
        private final Parameters.IntegerParameter regionHeightParam;
        private final Parameters.IntegerParameter inputSpaceWidthParam;
        private final Parameters.IntegerParameter inputSpaceHeightParam;
        private final DoubleParameter learningRadiusParam;
        private final DoubleParameter inputRadiusParam;
        private final Parameters.IntegerParameter cellsInColumnParam;
        private final JCheckBox skipSpatialCb;

        RegionParameters(Layer.Config cfg) {
            setLayout(new SpringLayout());
            regionWidthParam = new IntegerParameter(1, 50, cfg.getRegionDimension().width);
            regionHeightParam = new IntegerParameter(1, 50, cfg.getRegionDimension().height);
            inputSpaceWidthParam = new IntegerParameter(1, 50, cfg.getSensoryInputDimension().width);
            inputSpaceHeightParam = new IntegerParameter(1, 50, cfg.getSensoryInputDimension().height);
            learningRadiusParam = new DoubleParameter(1.0, 50.0, cfg.getLearningRadius(), 200);
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

        Layer.Config getParameters() {
            return new Layer.Config(new Dimension(regionWidthParam.getValue(),
                                                  regionHeightParam.getValue()),
                                    new Dimension(inputSpaceWidthParam.getValue(),
                                                  inputSpaceHeightParam.getValue()),
                                    inputRadiusParam.getValue(),
                                    learningRadiusParam.getValue(),
                                    skipSpatialCb.isSelected(),
                                    cellsInColumnParam.getValue());
        }

        void setParameters(Layer.Config cfg) {
            regionWidthParam.setValue(cfg.getRegionDimension().width);
            regionHeightParam.setValue(cfg.getRegionDimension().height);
            inputSpaceWidthParam.setValue(cfg.getSensoryInputDimension().width);
            inputSpaceHeightParam.setValue(cfg.getSensoryInputDimension().height);
            inputRadiusParam.setValue(cfg.getInputRadius());
            learningRadiusParam.setValue(cfg.getLearningRadius());
            skipSpatialCb.setSelected(cfg.isSkipSpatial());
        }
    }

    static class SpatialPoolerParameters extends JPanel {
        private final Parameters.IntegerParameter minOverlapParam;
        private final Parameters.IntegerParameter desiredLocalActivityParam;
        private final Parameters.DoubleParameter boostRateParam;

        SpatialPoolerParameters(SpatialPooler.Config cfg) {
            minOverlapParam = new IntegerParameter(1, 10, cfg.getMinOverlap());
            desiredLocalActivityParam = new IntegerParameter(1, 10, cfg.getDesiredLocalActivity());
            boostRateParam = new Parameters.DoubleParameter(0.005, 0.2, cfg.getBoostRate(), 200);
            setLayout(new SpringLayout());
            JLabel l = new FixedWidthLabel("Min Overlap");
            this.add(l);
            this.add(minOverlapParam);
            l = new FixedWidthLabel("Desired Local Activity");
            this.add(l);
            this.add(desiredLocalActivityParam);
            l = new FixedWidthLabel("Boost Rate");
            this.add(l);
            this.add(boostRateParam);
            UIUtils.makeSpringCompactGrid(this,
                                          3, 2, //rows, cols
                                          6, 6,        //initX, initY
                                          6, 6);       //xPad, yPad
        }

        SpatialPooler.Config getParameters() {
            return new SpatialPooler.Config(minOverlapParam.getValue(),
                                            desiredLocalActivityParam.getValue(),
                                            boostRateParam.getValue());
        }

        void setParameters(SpatialPooler.Config cfg) {
            minOverlapParam.setValue(cfg.getMinOverlap());
            desiredLocalActivityParam.setValue(cfg.getDesiredLocalActivity());
            boostRateParam.setValue(cfg.getBoostRate());
        }
    }

    static class ColumnParameters extends JPanel {
        private final Parameters.IntegerParameter amountOfProximalSynapsesParam;


        ColumnParameters(Column.Config cfg) {
            setLayout(new SpringLayout());
            amountOfProximalSynapsesParam = new IntegerParameter(2, 60, cfg.getAmountOfProximalSynapses());
            JLabel l = new FixedWidthLabel("N of Proximal Synapses");
            this.add(l);
            this.add(amountOfProximalSynapsesParam);
            UIUtils.makeSpringCompactGrid(this,
                                          1, 2, //rows, cols
                                          6, 6,        //initX, initY
                                          6, 6);       //xPad, yPad
        }

        Column.Config getParameters() {
            return new Column.Config(amountOfProximalSynapsesParam.getValue());
        }

        void setParameters(Column.Config cfg) {
            amountOfProximalSynapsesParam.setValue(cfg.getAmountOfProximalSynapses());
        }
    }

    static class TemporalPoolerParameters extends JPanel {
        private final Parameters.IntegerParameter newSynapseCountParam;
        private final Parameters.IntegerParameter activationThresholdParam;
        private final Parameters.IntegerParameter minThresholdParam;

        TemporalPoolerParameters(TemporalPooler.Config temporalPoolerCfg) {
            setLayout(new SpringLayout());
            newSynapseCountParam = new IntegerParameter(1, 10, temporalPoolerCfg.getNewSynapseCount());
            activationThresholdParam = new IntegerParameter(0, 15, temporalPoolerCfg.getActivationThreshold());
            minThresholdParam = new IntegerParameter(0, 5, temporalPoolerCfg.getMinThreshold());
            JLabel l = new FixedWidthLabel("N of New Synapses");
            this.add(l);
            this.add(newSynapseCountParam);
            l = new FixedWidthLabel("Activation Threshold");
            this.add(l);
            this.add(activationThresholdParam);
            l = new FixedWidthLabel("Minimum Threshold");
            this.add(l);
            this.add(minThresholdParam);
            UIUtils.makeSpringCompactGrid(this,
                                          3, 2, //rows, cols
                                          6, 6,        //initX, initY
                                          6, 6);       //xPad, yPad
        }


        TemporalPooler.Config getParameters() {
            return new TemporalPooler.Config(newSynapseCountParam.getValue(),
                                             activationThresholdParam.getValue(),
                                             minThresholdParam.getValue());
        }

        void setParameters(TemporalPooler.Config temporalPoolerCfg) {
            newSynapseCountParam.setValue(temporalPoolerCfg.getNewSynapseCount());
            activationThresholdParam.setValue(temporalPoolerCfg.getActivationThreshold());
            minThresholdParam.setValue(temporalPoolerCfg.getMinThreshold());
        }
    }

    static class CellParameters extends JPanel {

        private final Parameters.IntegerParameter amountOfSynapsesParam;
        private final Parameters.IntegerParameter timeStepsParam;

        CellParameters(Cell.Config cellCfg) {
            setLayout(new SpringLayout());
            amountOfSynapsesParam = new IntegerParameter(5, 60, cellCfg.getAmountOfSynapses());
            timeStepsParam = new IntegerParameter(2, 30, cellCfg.getTimeSteps());
            JLabel l = new FixedWidthLabel("Amount of Synapses");
            this.add(l);
            this.add(amountOfSynapsesParam);
            l = new FixedWidthLabel("Time Buffer");
            this.add(l);
            this.add(timeStepsParam);
            UIUtils.makeSpringCompactGrid(this,
                                          2, 2, //rows, cols
                                          6, 6,        //initX, initY
                                          6, 6);       //xPad, yPad
        }

        Cell.Config getParameters() {
            return new Cell.Config(amountOfSynapsesParam.getValue(),
                                   timeStepsParam.getValue());
        }


        void setParameters(Cell.Config cfg) {
            amountOfSynapsesParam.setValue(cfg.getAmountOfSynapses());
            timeStepsParam.setValue(cfg.getTimeSteps());
        }
    }


    static class SynapseParameters extends JPanel {
        private final Parameters.DoubleParameter connectedPermanenceParam;
        private final Parameters.DoubleParameter incPermanenceParam;
        private final Parameters.DoubleParameter decPermanenceParam;

        SynapseParameters(Synapse.Config cfg) {
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
