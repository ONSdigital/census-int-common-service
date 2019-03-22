package uk.gov.ons.ctp.common.util;

// import net.sourceforge.cobertura.CoverageIgnore;

/** Class to collect together some useful string manipulation methods */
// @CoverageIgnore
public class StringUtils {

  /**
   * Take a string and split into a number of equally sized segments
   *
   * @param text the string to split
   * @param size the segment size
   * @return the array of segments
   */
  public static String[] splitEqually(String text, int size) {
    String[] ret = new String[text.length() / size];

    for (int start = 0, segment = 0; start < text.length(); start += size, segment++) {
      ret[segment] = (text.substring(start, Math.min(text.length(), start + size)));
    }
    return ret;
  }

  /**
   * Calculate the ordinal position of a char in a char array
   *
   * @param arr the array
   * @param c the char
   * @return the numeric index of that char in the array
   */
  public static int indexOf(char[] arr, char c) {
    int ret = -1;
    if (arr != null) {
      for (int n = 0; n < arr.length; n++) {
        if (arr[n] == c) {
          ret = n;
          break;
        }
      }
    }
    return ret;
  }

  /**
   * This method takes multiple strings and returns the first one which is not null and also not
   * empty.
   *
   * @param candidateStrings, contains 1 or more strings.
   * @return the first non-null and non-empty String, or an empty string if none of candidateStrings
   *     are suitable.
   */
  public static String selectFirstNonBlankString(String... candidateStrings) {
    String preferredString = "";

    // Use the first non empty string
    for (String candidateString : candidateStrings) {
      if (candidateString != null && !candidateString.trim().isEmpty()) {
        preferredString = candidateString.trim();
        break;
      }
    }

    return preferredString;
  }
}
