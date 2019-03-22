package uk.gov.ons.ctp.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilsTest {
  @Test
  public void testSelectFirstNonBlankStringInitialMatch() {
    String result = StringUtils.selectFirstNonBlankString("x", null, "y");
    assertEquals("x", result);
  }

  @Test
  public void testSelectFirstNonBlankStringNotInitialMatch() {
    String result = StringUtils.selectFirstNonBlankString("", null, "", "", "zob", "tree");
    assertEquals("zob", result);
  }

  @Test
  public void testSelectFirstNonBlankStringWithNoArgs() {
    String result = StringUtils.selectFirstNonBlankString();
    assertEquals("", result);
  }

  @Test
  public void testSelectFirstNonBlankStringAllUnsuitable() {
    String result = StringUtils.selectFirstNonBlankString("", null, "", null);
    assertEquals("", result);
  }
}
