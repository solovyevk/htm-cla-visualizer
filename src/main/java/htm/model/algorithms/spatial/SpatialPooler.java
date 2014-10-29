/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.model.algorithms.spatial;

public abstract class SpatialPooler extends htm.model.algorithms.Pooler {

  private final int minimalOverlap;

  private final double boostRate;

  private final int desiredLocalActivity;

  protected SpatialPooler(Config cfg) {
    this.minimalOverlap = cfg.getMinOverlap();
    this.boostRate = cfg.getBoostRate();
    this.desiredLocalActivity = cfg.getDesiredLocalActivity();
  }

  /**
   * WP
   * A minimum number of inputs that must be active for a
   * column to be considered during the inhibition step.
   */
  public int getMinimalOverlap() {
    return minimalOverlap;
  }

  /**
   * The amount that is added to a Column's Boost value in a single time step, when it is being boosted.
   */
  public double getBoostRate() {
    return boostRate;
  }

  /**
   * WP
   * desiredLocalActivity A parameter controlling the number of columns that
   * will be winners after the inhibition step.
   */
  public int getDesiredLocalActivity() {
    return desiredLocalActivity;
  }

  public static class Config {
    private final int minOverlap;
    private final int desiredLocalActivity;
    private final double boostRate;

    public Config(int minOverlap, int desiredLocalActivity,
                  double boostRate) {
      this.minOverlap = minOverlap;
      this.desiredLocalActivity = desiredLocalActivity;
      this.boostRate = boostRate;
    }


    public int getMinOverlap() {
      return minOverlap;
    }

    public int getDesiredLocalActivity() {
      return desiredLocalActivity;
    }

    public double getBoostRate() {
      return boostRate;
    }
  }
}



