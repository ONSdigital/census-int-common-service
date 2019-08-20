package uk.gov.ons.ctp.common.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Custom Object Mapper */
@SuppressWarnings("serial")
public class CustomObjectMapper extends ObjectMapper {

  /** Custom Object Mapper Constructor */
  public CustomObjectMapper() {
    this.registerModule(new JavaTimeModule());
    this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.findAndRegisterModules();
  }
}
