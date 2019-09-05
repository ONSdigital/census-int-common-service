package uk.gov.ons.ctp.common.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Date;

/** Custom Object Mapper */
@SuppressWarnings("serial")
public class CustomObjectMapper extends ObjectMapper {

  /** Custom Object Mapper Constructor */
  public CustomObjectMapper() {
    this.registerModule(new JavaTimeModule());
    this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    SimpleModule module = new SimpleModule();
    module.addSerializer(Date.class, new CustomDateSerialiser());
    this.registerModule(module);

    this.findAndRegisterModules();
  }
}
