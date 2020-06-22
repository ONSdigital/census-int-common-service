package uk.gov.ons.ctp.common.cloud;

/**
 * This exception is thrown when we detect that the datastore is being overloaded.
 *
 * <p>It is not public, as it is part of the internal workings of the retryable data store.
 *
 * <p>This will allow data store methods which have the @Retryable annotation to use retry with an
 * exponential backoff.
 */
class DataStoreContentionException extends Exception {
  private static final long serialVersionUID = 4250385007849932900L;

  public DataStoreContentionException(String message, Exception e) {
    super(message, e);
  }
}
