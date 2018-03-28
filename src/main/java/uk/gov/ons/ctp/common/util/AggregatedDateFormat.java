package uk.gov.ons.ctp.common.util;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Date;

/**
 * This class is designed to provide more flexible parsing of dates than the standard DateFormats.  It accepts
 * an array of input formats that are attempted in order when parsing.  The value returned by the first input format
 * that successfully parses the date will be returned.  This class also contains an output format that is used
 * for formatting dates.
 */
@Slf4j
public class AggregatedDateFormat extends DateFormat {

    private DateFormat[] inputFormats;
    private DateFormat outputFormat;

    public AggregatedDateFormat(final DateFormat outputFormat, final DateFormat[] inputFormats) {
        this.inputFormats = inputFormats;
        this.outputFormat = outputFormat;
    }

    @Override
    public StringBuffer format(final Date date, final StringBuffer toAppendTo, final FieldPosition fieldPosition) {
        log.trace("Formatting: {}", date);
        return this.outputFormat.format(date, toAppendTo, fieldPosition);
    }

    @Override
    public Object clone() {
        return new AggregatedDateFormat(this.outputFormat, this.inputFormats);
    }

    @Override
    public Date parse(final String source, final ParsePosition pos) {
        log.trace("Parsing: {}", source);
        return Arrays.stream(this.inputFormats).map(d -> d.parse(source, pos)).findFirst().orElse(null);
    }
}
