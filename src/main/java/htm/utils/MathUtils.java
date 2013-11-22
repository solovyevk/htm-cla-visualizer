/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.utils;

import java.math.BigDecimal;

public class MathUtils {
  private MathUtils() {
  }

  static public double findMax(double... values) {
    double max = Double.MIN_VALUE;
    for (double d : values) {
      if (d > max) max = d;
    }
    return max;
  }

  static public double findMin(double... values) {
    double min = Double.MAX_VALUE;
    for (double d : values) {
      if (d < min) min = d;
    }
    return min;
  }

  public static boolean inRange(int value, int lowerBound, int upperBound) {
    return (lowerBound <= value && value <= upperBound);
  }

  public static boolean inRange(double value, double lowerBound, double upperBound) {
     return (lowerBound <= value && value <= upperBound);
   }

  public static double round(double value, int places) {
      if (places < 0) throw new IllegalArgumentException("places should not be negative");
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(places, BigDecimal.ROUND_HALF_UP);
      return bd.doubleValue();
  }

}
