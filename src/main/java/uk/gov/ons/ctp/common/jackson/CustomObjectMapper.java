package uk.gov.ons.ctp.common.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.ons.ctp.common.util.MultiIsoDateFormat;

/** Custom Object Mapper */
public class CustomObjectMapper extends ObjectMapper {

  /** Custom Object Mapper Constructor */
  public CustomObjectMapper() {
    this.setDateFormat(new MultiIsoDateFormat());
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
}
