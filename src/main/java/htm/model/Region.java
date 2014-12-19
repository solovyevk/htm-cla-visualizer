/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model;

import java.util.HashMap;
import java.util.Map;

public class Region extends htm.model.fractal.Composite<Region, Region> {
  public static final int BRAIN_REGION_LAYERS_NUMBER = 6;

  protected final Map<String, Layer> layers = new HashMap<String, Layer>(BRAIN_REGION_LAYERS_NUMBER);

  public  Layer getLayer(String level){
    return layers.get(level);
  }

}

