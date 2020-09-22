package uk.gov.ons.ctp.common.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EstabTypeTest {

  @Test
  public void canFindHousehold() {
    EstabType hhLowerEstabType = EstabType.forCode("Household");
    assertEquals(EstabType.HOUSEHOLD, hhLowerEstabType);

    EstabType hhUpperEstabType = EstabType.forCode("HOUSEHOLD");
    assertEquals(EstabType.HOUSEHOLD, hhUpperEstabType);
  }

  @Test
  public void canFindShelteredAccomodation() {
    EstabType saLowerEstabType = EstabType.forCode("Sheltered Accommodation");
    assertEquals(EstabType.SHELTERED_ACCOMMODATION, saLowerEstabType);

    EstabType saUpperEstabType = EstabType.forCode("SHELTERED ACCOMMODATION");
    assertEquals(EstabType.SHELTERED_ACCOMMODATION, saUpperEstabType);
  }

  @Test
  public void estabIsNotSecure() {
    EstabType careHome = EstabType.forCode("Care home");
    assertFalse(careHome.isSecure());
  }

  @Test
  public void estabIsSecure() {
    EstabType mentalHealthEstab = EstabType.forCode("HIGH SECURE MENTAL HEALTH");
    assertTrue(mentalHealthEstab.isSecure());
  }

  @Test
  public void unknownTurnsIntoOther() {
    EstabType otherEstabType = EstabType.forCode("Floating Caravan Palace");
    // assert that unrecognised yields OTHER
    assertEquals(EstabType.OTHER, otherEstabType);
    // and that OTHER addressType is empty optional
    assert (otherEstabType.getAddressType().isEmpty());
  }
}
