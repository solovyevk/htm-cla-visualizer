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
  Map<String, Layer> layers = new HashMap<String, Layer>();

  public  Layer getLayer(String level){
    return layers.get(level);
  }

}

