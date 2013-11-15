/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer;

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
  private static final String INPUT_SPACE_ELEMENT = "inputSpace";
  private static final String INPUT_RADIUS_ELEMENT = "inputRadius";
  private static final String X_SIZE = "sizeX";
  private static final String Y_SIZE = "sizeY";
  private static final String PATTERNS_LIST_ELEMENT_NAME = "patternsList";
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
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    boolean parseRegion = false;
    XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
    // read the XML document
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
                .equals(REGION_ELEMENT)) {
          parseRegion = false;
        }
      }
    }
    if (inputRadius == -1 || regionDimension.height == -1 || regionDimension.width == -1 || inputSpaceDimension.height == -1 || inputSpaceDimension.width == -1) {
      throw new IllegalArgumentException("Can't find HTM necessary parameters in input file");
    }
    return new HTMGraphicInterface.Config(patterns, regionDimension, inputSpaceDimension, inputRadius);
  }

  public void saveHTMParameters(File output, HTMGraphicInterface htmInterface) throws Exception {
    OutputStream out = new FileOutputStream(output);
    try {
      saveHTMParameters(out, htmInterface);
    } finally {
      out.close();
    }
  }

  public void saveHTMParameters(OutputStream out, HTMGraphicInterface htmInterface) throws Exception {
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
    createNode(eventWriter, INPUT_RADIUS_ELEMENT, htmInterface.getRegion().getInputRadius() + "");
    createNode(eventWriter, X_SIZE, htmInterface.getRegion().getDimension().width + "");
    createNode(eventWriter, Y_SIZE, htmInterface.getRegion().getDimension().height + "");
    eventWriter.add(eventFactory.createEndElement("", "", REGION_ELEMENT));
    eventWriter.add(end);
    eventWriter.add(eventFactory.createStartElement("", "", INPUT_SPACE_ELEMENT));
    eventWriter.add(end);
    createNode(eventWriter, X_SIZE, htmInterface.getSensoryInput().getDimension().width + "");
    createNode(eventWriter, Y_SIZE, htmInterface.getSensoryInput().getDimension().height + "");
    eventWriter.add(eventFactory.createEndElement("", "", INPUT_SPACE_ELEMENT));
    eventWriter.add(end);
    eventWriter.add(eventFactory.createStartElement("", "", PATTERNS_LIST_ELEMENT_NAME));
    eventWriter.add(end);
    // Write patterns
    for (boolean[] pattern : htmInterface.getPatterns()) {
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
