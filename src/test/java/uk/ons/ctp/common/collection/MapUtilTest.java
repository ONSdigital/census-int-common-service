package uk.ons.ctp.common.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.junit.Test;
import uk.gov.ons.ctp.common.collection.MapUtil;

public class MapUtilTest {

  public Map<String, String> testMap = new HashMap<>();

  @Test
  public void putIfNotNullWhenNotNull() {
    MapUtil.putIfNotNull(testMap, "notNull", () -> "notNullValue");
    assertEquals(testMap.get("notNull"), "notNullValue");
  }

  @Test
  public void putIfNotNullWhenDirectlyNull() {
    MapUtil.putIfNotNull(testMap, "directlyNull", () -> null);
    assertFalse(testMap.containsKey("directlyNull"));
  }

  @Test
  public void putIfNotNullWhenInDirectlyNull() {
    Foo foo = new Foo();
    MapUtil.putIfNotNull(testMap, "inDirectlyNull", () -> foo.getName());
    assertFalse(testMap.containsKey("inDirectlyNull"));
  }

  @Test
  public void putIfNotNullWhenNullPointer() {
    Foo foo = new Foo();
    MapUtil.putIfNotNull(testMap, "inDirectlyNull", () -> foo.getChild().getName());
    assertFalse(testMap.containsKey("inDirectlyNull"));
  }

  @Data
  class Foo {
    String name;
    Foo child;
  }
}
