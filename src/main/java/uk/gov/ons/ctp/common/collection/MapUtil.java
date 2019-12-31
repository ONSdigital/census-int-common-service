package uk.gov.ons.ctp.common.collection;

import java.util.Map;
import java.util.function.Supplier;

public interface MapUtil {

  /**
   * Using the supplied lambda, put the value it provides into the map IF the value is non null AND
   * the obtaining of the value does not yield a NullPointerException (allows for
   * youTo.getPossibleNullFirst().beforeUsingValue() ELSE do not put an entry into the map with that
   * key
   *
   * <p>NOTE: the provided lambda cannot throw checked exceptions, so if your required lambda
   * declares such, extract that into a prior step before calling this method.
   *
   * @param map the map to put into
   * @param key the key for the value in the map
   * @param supplier a lambda whose result will be put into the map against the key
   */
  static <K, V> void putIfNotNull(Map<K, V> map, K key, Supplier<V> supplier) {
    try {
      map.computeIfAbsent(key, (v) -> supplier.get());
    } catch (NullPointerException e) {
      // do nothing - the whole point
    }
  }
}
