package uk.gov.ons.ctp.common.cloud;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

@RunWith(MockitoJUnitRunner.class)
public class RetryableCloudDataStoreTest extends CloudTestBase {

  @Mock private CloudDataStore cloudDataStore;

  private RetryableCloudDataStoreImpl.Retrier retrier;

  @InjectMocks private RetryableCloudDataStoreImpl retryDataStoreImpl;

  private RetryableCloudDataStore retryDataStore;
  private RetryConfig retryConfig = new RetryConfig();

  @Before
  public void setup() {
    retrier = new RetryableCloudDataStoreImpl.Retrier(cloudDataStore, retryConfig);
    ReflectionTestUtils.setField(retryDataStoreImpl, "retrier", retrier);
    retryDataStore = retryDataStoreImpl;
  }

  @Test
  public void shouldListCollectionNames() {
    Set<String> names = Arrays.asList("Fred", "Julie", "Tyrone").stream().collect(toSet());
    when(cloudDataStore.getCollectionNames()).thenReturn(names);
    assertEquals(names, retryDataStore.getCollectionNames());
  }

  @Test(expected = CTPException.class)
  public void shouldRethrowOnRecover() throws Exception {
    retryDataStoreImpl.doRecover(new CTPException(Fault.SYSTEM_ERROR));
  }

  @Test
  public void shouldSearch() throws Exception {
    List<DummyCase> mockResults = Arrays.asList(CASE1, CASE2).stream().collect(Collectors.toList());
    String[] searchCriteria = new String[] {"contact", "surname"};
    when(cloudDataStore.search(DummyCase.class, TEST_SCHEMA, searchCriteria, "Smith"))
        .thenReturn(mockResults);
    List<DummyCase> results =
        retryDataStore.search(DummyCase.class, TEST_SCHEMA, searchCriteria, "Smith");
    assertEquals(mockResults, results);
  }

  @Test
  public void shouldRetrieveCase() throws Exception {
    when(cloudDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, CASE1.getId()))
        .thenReturn(Optional.of(CASE1));
    Optional<DummyCase> retrievedCase =
        retryDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, CASE1.getId());
    assertEquals(CASE1, retrievedCase.get());
  }

  @Test
  public void shouldRetrieveNothing() throws Exception {
    when(cloudDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, CASE1.getId()))
        .thenReturn(Optional.empty());
    Optional<DummyCase> retrievedCase =
        retryDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, CASE1.getId());
    assertTrue(retrievedCase.isEmpty());
  }

  @Test
  public void shouldStore() throws Exception {
    retryDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1, "a case");
    verify(cloudDataStore).storeObject(eq(TEST_SCHEMA), eq(CASE1.getId()), eq(CASE1));
  }

  @Test
  public void shouldThrowCtpExceptionWhenRetriesExhausted() throws Exception {
    doThrow(new DataStoreContentionException("argh", new Exception()))
        .when(cloudDataStore)
        .storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);
    try {
      retryDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1, "a case");
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.SYSTEM_ERROR, e.getFault());
      assertEquals("Retries exhausted for storage of DummyCase: a case", e.getMessage());
    }
  }
}
