package uk.gov.ons.ctp.common.util;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import java.util.UUID;

/**
 * Bi-directional String to UUID Converter
 */
public class StringToUUIDConverter extends BidirectionalConverter<String, UUID> {

  /**
   * Converts String to UUID
   * @param source String to use
   * @param destinationType currently unused
   * @param mappingContext currently unused
   * @return UUID String as UUID
   */
  public UUID convertTo(String source, Type<UUID> destinationType, MappingContext mappingContext) {
    return UUID.fromString(source);
  }

  /**
   * Converts UUID to String
   * @param source UUID to use
   * @param destinationType currently unused
   * @param mappingContext currently unused
   * @return String UUID as String
   */
  public String convertFrom(UUID source, Type<String> destinationType, MappingContext mappingContext) {
    return source.toString();
  }
}
