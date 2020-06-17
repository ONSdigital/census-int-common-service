package uk.gov.ons.ctp.common.cloud;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import uk.gov.ons.ctp.common.error.CTPException;

public interface CloudDataStore {

  void connect();

  void storeObject(final String schema, final String key, final Object value)
      throws CTPException, DataStoreContentionException;

  <T> Optional<T> retrieveObject(Class<T> target, final String schema, final String key)
      throws CTPException;

  <T> List<T> search(Class<T> target, final String schema, String[] fieldPath, String searchValue)
      throws CTPException;

  void deleteObject(final String schema, final String key) throws CTPException;

  Set<String> getCollectionNames();
}
