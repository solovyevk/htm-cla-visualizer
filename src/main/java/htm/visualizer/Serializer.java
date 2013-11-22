/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer;

import htm.model.Column;
import htm.model.Region;
import htm.model.Synapse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public enum Serializer {
  INSTANCE;
  private static final Log LOG = LogFactory.getLog(Serializer.class);
  private static final String HTM_ELEMENT = "htm-config";
  private static final String REGION_ELEMENT = "region";
  private static final String COLUMN_ELEMENT = "column";
  private static final String PROXIMAL_SYNAPSE_ELEMENT = "proximalSynapse";
  private static final String DISTAL_SYNAPSE_ELEMENT = "distalSynapse";

  private static final String SKIP_SPATIAL_POOLING_ELEMENT = "skipSpatial";
  private static final String INPUT_SPACE_ELEMENT = "inputSpace";
  private static final String INPUT_RADIUS_ELEMENT = "inputRadius";
  private static final String X_SIZE = "sizeX";
  private static final String Y_SIZE = "sizeY";
  private static final String PATTERNS_LIST_ELEMENT_NAME = "patternsList";

  private static final String CELLS_IN_COLUMN_ELEMENT = "cellsInColumn";
  private static final String AMOUNT_OF_PROXIMAL_SYNAPSES_ELEMENT = "amountOfProximalSynapses";
  private static final String MIN_OVERLAP_ELEMENT = "minOverlap";
  private static final String DESIRED_LOCAL_ACTIVITY_ELEMENT = "desiredLocalActivity";
  private static final String BOOST_RATE_ELEMENT = "boostRate";

  private static final String CONNECTED_PERMANENCE_ELEMENT = "connectedPerm";
  private static final String PERMANENCE_INCREASE_ELEMENT = "permanenceInc";
  private static final String PERMANENCE_DECREASE_ELEMENT = "permanenceDec";


  private static final String PATTERN_ELEMENT_NAME = "pattern";

  public HTMGraphicInterface.Config loadHTMParameters(File input) throws Exception {
    InputStream in = new FileInputStream(input);
    try {
      return loadHTMParameters(in);
    } finally {
      in.close();
    }
  }

  public HTMGraphicInterface.Config loadHTMParameters(InputStream in) throws Exception {
    List<boolean[]> patterns = new ArrayList<boolean[]>();
    double inputRadius = -1;
    Dimension regionDimension = new Dimension(-1, -1);
    Dimension inputSpaceDimension = new Dimension(-1, -1);
    int cellsInColumn = -1;
    int amountOfProximalSynapses = -1;
    int minOverlap = -1;
    int desiredLocalActivity = -1;
    double boostRate = -1.0;
    double proximalConnectedPerm = -1.0;
    double proximalPermanenceInc = -1.0;
    double proximalPermanenceDec = -1.0;
    double distalConnectedPerm = -1.0;
    double distalPermanenceInc = -1.0;
    double distalPermanenceDec = -1.0;
    boolean skipSpatialPooling = false;


    boolean parseRegion = false;
    boolean parseProximalSynapse = false;
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();
      if (event.isStartElement()) {
        if (event.asStartElement().getName().getLocalPart()
                .equals(INPUT_RADIUS_ELEMENT)) {
          event = eventReader.nextEvent();
          inputRadius = Double.parseDouble(event.asCharacters().getData());
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(REGION_ELEMENT)) {
          parseRegion = true;
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(PROXIMAL_SYNAPSE_ELEMENT)) {
          parseProximalSynapse = true;
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(CONNECTED_PERMANENCE_ELEMENT)) {
          event = eventReader.nextEvent();
          double connectedPerm = Double.parseDouble(event.asCharacters().getData());
          if (parseProximalSynapse) {
            proximalConnectedPerm = connectedPerm;
          } else {
            distalConnectedPerm = connectedPerm;
          }
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(PERMANENCE_INCREASE_ELEMENT)) {
          event = eventReader.nextEvent();
          double permanenceInc = Double.parseDouble(event.asCharacters().getData());
          if (parseProximalSynapse) {
            proximalPermanenceInc = permanenceInc;
          } else {
            distalPermanenceInc = permanenceInc;
          }
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(PERMANENCE_DECREASE_ELEMENT)) {
          event = eventReader.nextEvent();
          double permanenceDec = Double.parseDouble(event.asCharacters().getData());
          if (parseProximalSynapse) {
            proximalPermanenceDec = permanenceDec;
          } else {
            distalPermanenceDec = permanenceDec;
          }
          continue;
        }

        if (event.asStartElement().getName().getLocalPart()
                .equals(CELLS_IN_COLUMN_ELEMENT)) {
          event = eventReader.nextEvent();
          cellsInColumn = Integer.parseInt(event.asCharacters().getData());
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(AMOUNT_OF_PROXIMAL_SYNAPSES_ELEMENT)) {
          event = eventReader.nextEvent();
          amountOfProximalSynapses = Integer.parseInt(event.asCharacters().getData());
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(MIN_OVERLAP_ELEMENT)) {
          event = eventReader.nextEvent();
          minOverlap = Integer.parseInt(event.asCharacters().getData());
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(DESIRED_LOCAL_ACTIVITY_ELEMENT)) {
          event = eventReader.nextEvent();
          desiredLocalActivity = Integer.parseInt(event.asCharacters().getData());
          continue;
        }

        if (event.asStartElement().getName().getLocalPart()
                .equals(BOOST_RATE_ELEMENT)) {
          event = eventReader.nextEvent();
          boostRate = Double.parseDouble(event.asCharacters().getData());
          continue;
        }


        if (event.asStartElement().getName().getLocalPart()
                .equals(SKIP_SPATIAL_POOLING_ELEMENT)) {
          event = eventReader.nextEvent();
          skipSpatialPooling = event.asCharacters().getData().equals("true");
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(X_SIZE)) {
          event = eventReader.nextEvent();
          int x = Integer.parseInt(event.asCharacters().getData());
          if (parseRegion) {
            regionDimension.width = x;
          } else {
            inputSpaceDimension.width = x;
          }
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(Y_SIZE)) {
          event = eventReader.nextEvent();
          int y = Integer.parseInt(event.asCharacters().getData());
          if (parseRegion) {
            regionDimension.height = y;
          } else {
            inputSpaceDimension.height = y;
          }
          continue;
        }
        if (event.asStartElement().getName().getLocalPart()
                .equals(PATTERN_ELEMENT_NAME)) {
          event = eventReader.nextEvent();
          patterns.add(convertStringToPattern(event.asCharacters().getData()));
        }
      }
      if (event.isEndElement()) {
        if (event.asEndElement().getName().getLocalPart()
                .equals(PROXIMAL_SYNAPSE_ELEMENT)) {
          parseProximalSynapse = false;
          continue;
        }
        if (event.asEndElement().getName().getLocalPart()
                .equals(REGION_ELEMENT)) {
          parseRegion = false;
        }
      }
    }
    if (inputRadius == -1 || regionDimension.height == -1 || regionDimension.width == -1 || inputSpaceDimension.height == -1 || inputSpaceDimension.width == -1
        || cellsInColumn == -1 || amountOfProximalSynapses == -1 || minOverlap == -1 || desiredLocalActivity == -1
        || boostRate == -1.0 || proximalConnectedPerm == -1.0 || proximalPermanenceInc == -1.0
        || proximalPermanenceDec == -1.0 || distalConnectedPerm == -1.0 || distalPermanenceInc == -1.0
        || distalPermanenceDec == -1.0) {
      throw new IllegalArgumentException("Can't find HTM necessary parameters in input file");
    }
    return new HTMGraphicInterface.Config(patterns, new Region.Config(regionDimension, inputSpaceDimension, inputRadius,
                                                                      skipSpatialPooling), new Column.Config(
            cellsInColumn,
            amountOfProximalSynapses,
            minOverlap,
            desiredLocalActivity, boostRate), new Synapse.Config(proximalConnectedPerm, proximalPermanenceInc,
                                                                 proximalPermanenceDec),
                                          new Synapse.Config(distalConnectedPerm, distalPermanenceInc,
                                                             distalPermanenceDec));
  }

  public void saveHTMParameters(File output, HTMGraphicInterface.Config parameters) throws Exception {
    OutputStream out = new FileOutputStream(output);
    try {
      saveHTMParameters(out, parameters);
    } finally {
      out.close();
    }
  }

  public void saveHTMParameters(OutputStream out, HTMGraphicInterface.Config parameters) throws Exception {
    Region.Config regionCfg = parameters.getRegionConfig();
    Column.Config columnCfg = parameters.getColumnConfig();
    Synapse.Config proximalSynapseCfg = parameters.getProximalSynapseConfig();
    Synapse.Config distalSynapseCfg = parameters.getDistalSynapseConfig();
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    XMLEventWriter eventWriter = outputFactory
            .createXMLEventWriter(out);
    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
    XMLEvent end = eventFactory.createDTD("\n");
    StartDocument startDocument = eventFactory.createStartDocument();
    eventWriter.add(startDocument);
    eventWriter.add(end);
    eventWriter.add(eventFactory.createStartElement("", "", HTM_ELEMENT));
    eventWriter.add(end);

    eventWriter.add(eventFactory.createStartElement("", "", REGION_ELEMENT));
    eventWriter.add(end);
    createNode(eventWriter, SKIP_SPATIAL_POOLING_ELEMENT, regionCfg.isSkipSpatial() + "");
    createNode(eventWriter, INPUT_RADIUS_ELEMENT, regionCfg.getInputRadius() + "");
    createNode(eventWriter, X_SIZE, regionCfg.getRegionDimension().width + "");
    createNode(eventWriter, Y_SIZE, regionCfg.getRegionDimension().height + "");
    eventWriter.add(eventFactory.createEndElement("", "", REGION_ELEMENT));
    eventWriter.add(end);

    eventWriter.add(eventFactory.createStartElement("", "", INPUT_SPACE_ELEMENT));
    eventWriter.add(end);
    createNode(eventWriter, X_SIZE, regionCfg.getSensoryInputDimension().width + "");
    createNode(eventWriter, Y_SIZE, regionCfg.getSensoryInputDimension().height + "");
    eventWriter.add(eventFactory.createEndElement("", "", INPUT_SPACE_ELEMENT));
    eventWriter.add(end);

    eventWriter.add(eventFactory.createStartElement("", "", COLUMN_ELEMENT));
    eventWriter.add(end);
    createNode(eventWriter, CELLS_IN_COLUMN_ELEMENT, columnCfg.getCellsInColumn() + "");
    createNode(eventWriter, AMOUNT_OF_PROXIMAL_SYNAPSES_ELEMENT,
               columnCfg.getAmountOfProximalSynapses() + "");
    createNode(eventWriter, MIN_OVERLAP_ELEMENT, columnCfg.getMinOverlap() + "");
    createNode(eventWriter, DESIRED_LOCAL_ACTIVITY_ELEMENT,
               columnCfg.getDesiredLocalActivity() + "");
    createNode(eventWriter, BOOST_RATE_ELEMENT,
               columnCfg.getBoostRate() + "");
    eventWriter.add(eventFactory.createEndElement("", "", COLUMN_ELEMENT));
    eventWriter.add(end);

    eventWriter.add(eventFactory.createStartElement("", "", PROXIMAL_SYNAPSE_ELEMENT));
    eventWriter.add(end);
    createNode(eventWriter, CONNECTED_PERMANENCE_ELEMENT, proximalSynapseCfg.getConnectedPerm() + "");
    createNode(eventWriter, PERMANENCE_INCREASE_ELEMENT, proximalSynapseCfg.getPermanenceInc() + "");
    createNode(eventWriter, PERMANENCE_DECREASE_ELEMENT, proximalSynapseCfg.getPermanenceDec() + "");
    eventWriter.add(eventFactory.createEndElement("", "", PROXIMAL_SYNAPSE_ELEMENT));
    eventWriter.add(end);

    eventWriter.add(eventFactory.createStartElement("", "", DISTAL_SYNAPSE_ELEMENT));
    eventWriter.add(end);
    createNode(eventWriter, CONNECTED_PERMANENCE_ELEMENT, distalSynapseCfg.getConnectedPerm() + "");
    createNode(eventWriter, PERMANENCE_INCREASE_ELEMENT, distalSynapseCfg.getPermanenceInc() + "");
    createNode(eventWriter, PERMANENCE_DECREASE_ELEMENT, distalSynapseCfg.getPermanenceDec() + "");
    eventWriter.add(eventFactory.createEndElement("", "", DISTAL_SYNAPSE_ELEMENT));
    eventWriter.add(end);

    eventWriter.add(eventFactory.createStartElement("", "", PATTERNS_LIST_ELEMENT_NAME));
    eventWriter.add(end);
    // Write patterns
    for (boolean[] pattern : parameters.getPatterns()) {
      createNode(eventWriter, PATTERN_ELEMENT_NAME, convertPatternToString(pattern));
    }
    eventWriter.add(eventFactory.createEndElement("", "", PATTERNS_LIST_ELEMENT_NAME));
    eventWriter.add(end);
    eventWriter.add(eventFactory.createEndElement("", "", HTM_ELEMENT));
    eventWriter.add(end);
    eventWriter.add(eventFactory.createEndDocument());
    eventWriter.close();
  }

  private String convertPatternToString(boolean[] pattern) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < pattern.length; i++) {
      result.append(pattern[i] ? 1 : 0);
    }
    return result.toString();
  }

  private boolean[] convertStringToPattern(String patternStr) {
    boolean[] result = new boolean[patternStr.length()];
    for (int i = 0; i < patternStr.length(); i++) {
      result[i] = patternStr.charAt(i) == '1';
    }
    return result;
  }

  private void createNode(XMLEventWriter eventWriter, String name,
                          String value) throws XMLStreamException {

    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
    XMLEvent end = eventFactory.createDTD("\n");
    XMLEvent tab = eventFactory.createDTD("\t");
    // create Start node
    StartElement sElement = eventFactory.createStartElement("", "", name);
    eventWriter.add(tab);
    eventWriter.add(sElement);
    // create Content
    Characters characters = eventFactory.createCharacters(value);
    eventWriter.add(characters);
    // create End node
    EndElement eElement = eventFactory.createEndElement("", "", name);
    eventWriter.add(eElement);
    eventWriter.add(end);

  }
}
