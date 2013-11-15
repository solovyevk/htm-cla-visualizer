/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;

public enum UIUtils {
  INSTANCE;

  private static final Log LOG = LogFactory.getLog(UIUtils.class);
  private UIUtils() {
  }

  public ImageIcon createImageIcon(String path) {
    java.net.URL imgURL = getClass().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL);
    } else {
      LOG.error("Couldn't find file: " + path);
      throw new IllegalArgumentException("Couldn't find file: " + path);
    }
  }
}
