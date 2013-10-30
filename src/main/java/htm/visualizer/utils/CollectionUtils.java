/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package htm.visualizer.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionUtils {

  private CollectionUtils() {
  }

  public static <T> List<T> filter(Collection<T> target, Predicate<T> predicate) {
       List<T> result = new ArrayList<T>();
       for (T element: target) {
           if (predicate.apply(element)) {
               result.add(element);
           }
       }
       return result;
   }

  public static interface Predicate<T> { boolean apply(T type); }
}
