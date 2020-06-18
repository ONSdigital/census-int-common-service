package uk.gov.ons.ctp.common.cloud;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

/**
 * Decorator for <code>CloudDataStore</code>. It is responsible for handling exponential backoffs
 * when the datastore is becoming overloaded.
 */
@Service
public class RetryableCloudDataStoreImpl implements RetryableCloudDataStore {
  private static final Logger log = LoggerFactory.getLogger(RetryableCloudDataStoreImpl.class);

  private CloudDataStore cloudDataStore;

  @Autowired
  public RetryableCloudDataStoreImpl(CloudDataStore cloudDataStore) {
    this.cloudDataStore = cloudDataStore;
  }

  @Override
  public void storeObject(
      final String schema, final String key, final Object value, final String id)
      throws CTPException {
    try {
      this.storeWithRetry(schema, key, value);
    } catch (DataStoreContentionException e) {
      String identity = value.getClass().getName() + ": " + id;
      log.error("Retries exhausted for storage of {}", identity);
      throw new CTPException(Fault.SYSTEM_ERROR, e, "Retries exhausted for storage of " + identity);
    }
  }

  @Retryable(
      label = "storeObject",
      include = DataStoreContentionException.class,
      backoff =
          @Backoff(
              delayExpression = "#{${cloud-storage.backoff.initial}}",
              multiplierExpression = "#{${cloud-storage.backoff.multiplier}}",
              maxDelayExpression = "#{${cloud-storage.backoff.max}}"),
      maxAttemptsExpression = "#{${cloud-storage.backoff.max-attempts}}",
      listeners = "cloudRetryListener")
  public void storeWithRetry(final String schema, final String key, final Object value)
      throws CTPException, DataStoreContentionException {
    cloudDataStore.storeObject(schema, key, value);
  }

  @Override
  public <T> Optional<T> retrieveObject(Class<T> target, final String schema, final String key)
      throws CTPException {
    return cloudDataStore.retrieveObject(target, schema, key);
  }

  @Override
  public <T> List<T> search(
      Class<T> target, final String schema, String[] fieldPathElements, String searchValue)
      throws CTPException {
    return cloudDataStore.search(target, schema, fieldPathElements, searchValue);
  }

  @Override
  public Set<String> getCollectionNames() {
    return cloudDataStore.getCollectionNames();
  }

  /**
   * When attempts to retry object storage have been exhausted this method is invoked and it can
   * then throw the exception (triggering Rabbit retries). If this is not done then the message
   * won't be eligible for another attempt or writing to the dead letter queue.
   *
   * @param e is the final exception in the storeObject retries.
   * @throws Exception the exception which caused the final attempt to fail.
   */
  @Recover
  public void doRecover(Exception e) throws Exception {
    log.with(e.getMessage()).debug("Datastore recovery throwing exception");
    throw e;
  }
}
