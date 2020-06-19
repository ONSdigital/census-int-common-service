package uk.gov.ons.ctp.common.cloud;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class CloudTestBase {
  static final DummyCase CASE1 = new DummyCase("1", new DummyContact("jo", "Smith"));
  static final DummyCase CASE2 = new DummyCase("2", new DummyContact("Iain", "Smith"));
  static final String TEST_SCHEMA = "TEST_SCHEMA";

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class DummyCase {
    private String id;
    private DummyContact contact;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class DummyContact {
    private String forename;
    private String surname;
  }
}
