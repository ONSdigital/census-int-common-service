package uk.gov.ons.ctp.common.cloud;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import uk.gov.ons.ctp.common.error.CTPException;

/**
 * Abstraction for a document data store in the cloud, with robust retry capability during store
 * operation.
 *
 * <p>In order to use this class the client must apply the configuration annotation {@link
 * EnableRetry}.
 *
 * <p>Here is an example of the application properties that must be configured to control retry
 * characteristics (in a YAML style as you might place in Spring Boot <code>application.yaml</code>:
 *
 * <pre>
 * cloud-storage:
 *   backoff:
 *     initial: 100
 *     multiplier: 1.2
 *     max: 16000
 *     max-attempts: 30
 * </pre>
 *
 * <p>The example above, will retry a maximum of 30 times, or a maximum delay of 16 seconds. The
 * initial retry delay is 100 mSec. Each subsequent retry will be 1.2 times as long as the previous.
 *
 * <p>See {@link Backoff} annotation for more information on the backoff parameters.
 */
public interface RetryableCloudDataStore {

  /**
   * Write object to cloud collection. If the collection already holds an object with the specified
   * key then the contents of the value will be overwritten.
   *
   * <p>The implementation will employ a retry strategy if contention errors are detected.
   *
   * @param schema the name of the collection that the object will be added to.
   * @param key key for the object within the collection.
   * @param value the object to be written to the collection.
   * @param id a readable identifier for the object for error reporting. This may or may not be
   *     different from the <code>key</code> parameter; for instance if key were a hash then a
   *     different identifier may be better for the logged error/exception.
   * @throws CTPException an error has occurred, that could not be rectified by the retry strategy.
   */
  void storeObject(final String schema, final String key, final Object value, final String id)
      throws CTPException;

  /**
   * Read an object.
   *
   * @param <T> The object type that results should be returned in.
   * @param target the class of the object type that results should be returned in.
   * @param schema the name of the collection which holds the object.
   * @param key identifies the object within the collection.
   * @return Optional containing the object if it was found.
   * @throws CTPException on error
   */
  <T> Optional<T> retrieveObject(Class<T> target, final String schema, final String key)
      throws CTPException;

  /**
   * Runs an object search. This returns objects whose field is equal to the search value.
   *
   * @param <T> The object type that results should be returned in.
   * @param target the class of the object type that results should be returned in.
   * @param schema is the schema to search.
   * @param fieldPathElements is an array of strings that describe the path to the search field. eg,
   *     [ "case", "addresss", "postcode" ]
   * @param searchValue is the value that the field must equal for it to be returned as a result.
   * @return the list of results.
   * @throws CTPException on error
   */
  <T> List<T> search(
      Class<T> target, final String schema, String[] fieldPathElements, String searchValue)
      throws CTPException;

  /**
   * Get the names of top level cloud collections.
   *
   * @return a Set with the names of the current collections.
   */
  Set<String> getCollectionNames();
}
