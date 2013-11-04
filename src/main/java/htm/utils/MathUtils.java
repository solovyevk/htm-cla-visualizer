/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.utils;

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

}
