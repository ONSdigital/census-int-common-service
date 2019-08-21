package uk.gov.ons.ctp.common.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import uk.gov.ons.ctp.common.time.DateTimeUtil;

/** This class serialises dates to a standard application date format. */
public class CustomDateSerialiser extends JsonSerializer<Date> {

  private SimpleDateFormat dateFormatter;

  public CustomDateSerialiser() {
    dateFormatter = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @Override
  public void serialize(Date value, JsonGenerator jsonGenerator, SerializerProvider provider)
      throws IOException, JsonProcessingException {

    // Note that all threads are using the same formatter, which needs synchronisation as it is not
    // thread safe.
    synchronized (dateFormatter) {
      jsonGenerator.writeString(dateFormatter.format(value));
    }
  }
}
