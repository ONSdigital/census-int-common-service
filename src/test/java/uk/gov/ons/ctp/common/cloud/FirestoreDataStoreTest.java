package uk.gov.ons.ctp.common.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.error.CTPException;

@RunWith(MockitoJUnitRunner.class)
public class FirestoreDataStoreTest extends CloudTestBase {

  private static FirestoreDataStore firestoreDataStore = new FirestoreDataStore();

  @Mock private Firestore firestore;

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(firestoreDataStore, "firestore", firestore);
  }

  @Test
  public void testStoreObject() throws Exception {
    ApiFuture<WriteResult> apiFuture =
        mockFirestoreForExpectedStore(TEST_SCHEMA, CASE1.getId(), CASE1, null);

    firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);
    verify(apiFuture).get();
  }

  @Test
  public void testStoreObject_fails() throws Exception {
    ExecutionException firestoreException =
        new ExecutionException("fake Firestore exception", null);
    mockFirestoreForExpectedStore(TEST_SCHEMA, CASE1.getId(), CASE1, firestoreException);

    boolean exceptionCaught = false;
    try {
      firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Failed to create object"));
      assertTrue(
          e.getCause().getMessage(),
          e.getCause().getMessage().contains("fake Firestore exception"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testStoreObject_detectsContentionForResourceExhausted() throws Exception {
    doTestStoreObject_detectsContention(Status.RESOURCE_EXHAUSTED);
  }

  @Test
  public void testStoreObject_detectsContentionForAborted() throws Exception {
    doTestStoreObject_detectsContention(Status.ABORTED);
  }

  public void doTestStoreObject_detectsContention(Status status) throws Exception {
    // Build chain of exceptions as per Firestore
    Exception rootCauseException = new StatusRuntimeException(status);
    Exception causeException = new RuntimeException("e2", rootCauseException);
    Exception firestoreException =
        new java.util.concurrent.ExecutionException("e3", causeException);

    mockFirestoreForExpectedStore(TEST_SCHEMA, CASE1.getId(), CASE1, firestoreException);

    boolean exceptionCaught = false;
    try {
      firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);
    } catch (DataStoreContentionException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("contention on schema 'TEST_SCHEMA'"));
      exceptionCaught = true;
    }
    assertTrue("Failed to detect datastore contention", exceptionCaught);
  }

  @Test
  public void testStoreObject_doesNotIncorrectlyDetectContention() throws Exception {
    // Build chain of exceptions, but not with same values as Firestore uses for contention
    Exception rootCauseException = new StatusRuntimeException(Status.ALREADY_EXISTS);
    Exception causeException = new RuntimeException("e2", rootCauseException);
    Exception firestoreException =
        new java.util.concurrent.ExecutionException("e3", causeException);

    mockFirestoreForExpectedStore(TEST_SCHEMA, CASE1.getId(), CASE1, firestoreException);

    boolean contentionDetected = false;
    boolean ctpExceptionCreated = false;
    try {
      firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);
    } catch (DataStoreContentionException e) {
      contentionDetected = true;
    } catch (CTPException e) {
      ctpExceptionCreated = true;
    }
    assertFalse("Incorrectly diagnoised datastore contention", contentionDetected);
    assertTrue(ctpExceptionCreated);
  }

  @Test
  public void testRetrieveObject_found() throws Exception {
    mockFirestoreRetrieveObject(TEST_SCHEMA, CASE1.getId(), null, CASE1);

    // Verify that retrieveObject returns the expected result
    Optional<DummyCase> retrievedCase1 =
        firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, CASE1.getId());
    assertTrue(retrievedCase1.isPresent());
    assertEquals(CASE1, retrievedCase1.get());
  }

  @Test
  public void testRetrieveObject_notFound() throws Exception {
    String unknownId = UUID.randomUUID().toString();

    mockFirestoreRetrieveObject(TEST_SCHEMA, unknownId, null);

    // Submit a read for unknown object
    Optional<DummyCase> retrievedCase1 =
        firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, unknownId);

    // Verify that reading a non existent object returns an empty result set
    assertTrue(retrievedCase1.isEmpty());
  }

  @Test
  public void testRetrieveObject_failsWithMultipleResultsFound() throws Exception {
    // Force failure by making the retrieve return more than 1 result object
    mockFirestoreRetrieveObject(TEST_SCHEMA, CASE1.getId(), null, CASE1, CASE1);

    // Verify that retrieveObject fails because of more than 1 result
    boolean exceptionCaught = false;
    try {
      firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, CASE1.getId());
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Firestore returned more than 1"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testSearch_noResults() throws Exception {
    mockFirestoreSearch(TEST_SCHEMA, "Bob", null, null);

    // Verify that there are no results when searching for unknown forename
    String[] searchCriteria = new String[] {"contact", "forename"};
    List<DummyCase> retrievedCase1 =
        firestoreDataStore.search(DummyCase.class, TEST_SCHEMA, searchCriteria, "Bob");
    assertTrue(retrievedCase1.isEmpty());
  }

  @Test
  public void testSearch_singleResult() throws Exception {
    mockFirestoreSearch(TEST_SCHEMA, CASE1.getContact().getForename(), null, null, CASE1);

    // Verify that search can find the  first case
    String[] searchCriteria = new String[] {"contact", "forename"};
    List<DummyCase> retrievedCase1 =
        firestoreDataStore.search(
            DummyCase.class, TEST_SCHEMA, searchCriteria, CASE1.getContact().getForename());
    assertEquals(1, retrievedCase1.size());
    assertEquals(CASE1.getId(), retrievedCase1.get(0).getId());
    assertEquals(CASE1, retrievedCase1.get(0));
  }

  @Test
  public void testSearch_multipleResults() throws Exception {
    mockFirestoreSearch(TEST_SCHEMA, "Smith", null, null, CASE1, CASE2);

    // Verify that search can find the  first case
    String[] searchCriteria = new String[] {"contact", "surname"};
    List<DummyCase> retrievedCase1 =
        firestoreDataStore.search(DummyCase.class, TEST_SCHEMA, searchCriteria, "Smith");
    assertEquals(2, retrievedCase1.size());
    assertEquals(CASE1.getId(), retrievedCase1.get(0).getId());
    assertEquals(CASE1, retrievedCase1.get(0));
    assertEquals(CASE2.getId(), retrievedCase1.get(1).getId());
    assertEquals(CASE2, retrievedCase1.get(1));
  }

  @Test
  public void testSearch_failsWithFirestoreException() throws Exception {
    ExecutionException firestoreException =
        new ExecutionException("fake Firestore exception", null);
    mockFirestoreSearch(TEST_SCHEMA, "Smith", firestoreException, null, CASE1, CASE2);

    boolean exceptionCaught = false;
    try {
      String[] searchCriteria = new String[] {"contact", "surname"};
      firestoreDataStore.search(DummyCase.class, TEST_SCHEMA, searchCriteria, "Smith");
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Failed to search"));
      assertTrue(
          e.getCause().getMessage(),
          e.getCause().getMessage().contains("fake Firestore exception"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testSearch_failsSerialisationException() throws Exception {
    RuntimeException serialisationException = new RuntimeException("Could not deserialize object");
    mockFirestoreSearch(TEST_SCHEMA, "Smith", null, serialisationException, CASE1, CASE2);

    boolean exceptionCaught = false;
    try {
      String[] searchCriteria = new String[] {"contact", "surname"};
      firestoreDataStore.search(DummyCase.class, TEST_SCHEMA, searchCriteria, "Smith");
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Failed to convert"));
      assertTrue(
          e.getCause().getMessage(),
          e.getCause().getMessage().contains("Could not deserialize object"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testDelete_success() throws Exception {
    ApiFuture<WriteResult> apiFuture =
        mockFirestoreForExpectedDelete(TEST_SCHEMA, CASE1.getId(), null);

    // Delete it
    firestoreDataStore.deleteObject(TEST_SCHEMA, CASE1.getId());
    verify(apiFuture).get();
  }

  @Test
  public void testDelete_failsWithFirestoreException() throws Exception {
    ExecutionException firestoreException =
        new ExecutionException("fake Firestore exception", null);
    mockFirestoreForExpectedDelete(TEST_SCHEMA, CASE1.getId(), firestoreException);

    // Attempt a deletion
    boolean exceptionCaught = false;
    try {
      firestoreDataStore.deleteObject(TEST_SCHEMA, CASE1.getId());
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Failed to delete"));
      assertTrue(
          e.getCause().getMessage(),
          e.getCause().getMessage().contains("fake Firestore exception"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testDelete_onNonExistentObject() throws Exception {
    UUID nonExistantUUID = UUID.randomUUID();

    ApiFuture<WriteResult> apiFuture =
        mockFirestoreForAttemptedDelete(TEST_SCHEMA, nonExistantUUID.toString());

    // Attempt to delete a non existent object
    firestoreDataStore.deleteObject(TEST_SCHEMA, nonExistantUUID.toString());
    verify(apiFuture).get();
  }

  @Test
  public void testGetCollectionNames() {
    // Build names of collections that mock Firestore will list
    CollectionReference collectionA = Mockito.mock(CollectionReference.class);
    when(collectionA.getId()).thenReturn("collectionA");
    CollectionReference collectionB = Mockito.mock(CollectionReference.class);
    when(collectionB.getId()).thenReturn("collectionB");

    // Create results from listCollections
    Iterable<CollectionReference> collections = Arrays.asList(collectionA, collectionB);
    when(firestore.listCollections()).thenReturn(collections);

    Set<String> collectionNames = firestoreDataStore.getCollectionNames();

    assertTrue(collectionNames.toString(), collectionNames.contains("collectionA"));
    assertTrue(collectionNames.toString(), collectionNames.contains("collectionB"));
    assertEquals(2, collectionNames.size());
  }

  // --- helpers ...

  private ApiFuture<WriteResult> mockFirestoreForExpectedStore(
      String expectedSchema, String expectedKey, Object expectedValue, Exception exception)
      throws InterruptedException, ExecutionException {
    ApiFuture<WriteResult> apiFuture = genericMock(ApiFuture.class);
    if (exception == null) {
      when(apiFuture.get()).thenReturn(null);
    } else {
      when(apiFuture.get()).thenThrow(exception);
    }

    DocumentReference documentReference = Mockito.mock(DocumentReference.class);
    when(documentReference.set(eq(expectedValue))).thenReturn(apiFuture);

    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    when(collectionReference.document(eq(expectedKey))).thenReturn(documentReference);

    when(firestore.collection(eq(expectedSchema))).thenReturn(collectionReference);

    return apiFuture;
  }

  private void mockFirestoreRetrieveObject(
      String expectedSchema, String expectedSearchValue, Exception exception, DummyCase... case1)
      throws InterruptedException, ExecutionException {
    mockFirestoreSearch(expectedSchema, expectedSearchValue, exception, null, case1);
  }

  private void mockFirestoreSearch(
      String expectedSchema,
      String expectedSearchValue,
      Exception searchException,
      Exception serialisationException,
      DummyCase... resultData)
      throws InterruptedException, ExecutionException {

    ApiFuture<QuerySnapshot> apiFuture = genericMock(ApiFuture.class);

    if (searchException == null && serialisationException == null) {
      // Build list of results which are to be returned
      List<QueryDocumentSnapshot> results = new ArrayList<>();
      for (DummyCase caseObj : resultData) {
        QueryDocumentSnapshot doc1 = Mockito.mock(QueryDocumentSnapshot.class);
        when(doc1.toObject(eq(DummyCase.class))).thenReturn(caseObj);
        results.add(doc1);
      }

      QuerySnapshot querySnapshot = Mockito.mock(QuerySnapshot.class);
      when(querySnapshot.getDocuments()).thenReturn(results);

      when(apiFuture.get()).thenReturn(querySnapshot);
    } else if (searchException != null) {
      when(apiFuture.get()).thenThrow(searchException);
    } else {
      // SerialisationException
      List<QueryDocumentSnapshot> results = new ArrayList<>();
      QueryDocumentSnapshot doc1 = Mockito.mock(QueryDocumentSnapshot.class);
      when(doc1.toObject(eq(DummyCase.class))).thenThrow(serialisationException);
      results.add(doc1);

      QuerySnapshot querySnapshot = Mockito.mock(QuerySnapshot.class);
      when(querySnapshot.getDocuments()).thenReturn(results);

      when(apiFuture.get()).thenReturn(querySnapshot);
    }

    Query query = Mockito.mock(Query.class);
    when(query.get()).thenReturn(apiFuture);

    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    when(collectionReference.whereEqualTo((FieldPath) any(), eq(expectedSearchValue)))
        .thenReturn(query);

    when(firestore.collection(eq(expectedSchema))).thenReturn(collectionReference);
  }

  private ApiFuture<WriteResult> mockFirestoreForExpectedDelete(
      String expectedSchema, String expectedKey, Exception exception)
      throws InterruptedException, ExecutionException {
    ApiFuture<WriteResult> apiFuture = genericMock(ApiFuture.class);
    if (exception == null) {
      when(apiFuture.get()).thenReturn(null);
    } else {
      when(apiFuture.get()).thenThrow(exception);
    }

    DocumentReference documentReference = Mockito.mock(DocumentReference.class);
    when(documentReference.delete()).thenReturn(apiFuture);

    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    when(collectionReference.document(eq(expectedKey))).thenReturn(documentReference);

    when(firestore.collection(eq(expectedSchema))).thenReturn(collectionReference);

    return apiFuture;
  }

  private ApiFuture<WriteResult> mockFirestoreForAttemptedDelete(
      String expectedSchema, String expectedKey) throws InterruptedException, ExecutionException {
    ApiFuture<WriteResult> apiFuture = genericMock(ApiFuture.class);
    when(apiFuture.get()).thenReturn(null);

    DocumentReference documentReference = Mockito.mock(DocumentReference.class);
    when(documentReference.delete()).thenReturn(apiFuture);

    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    when(collectionReference.document(eq(expectedKey))).thenReturn(documentReference);

    when(firestore.collection(eq(expectedSchema))).thenReturn(collectionReference);

    return apiFuture;
  }

  @SuppressWarnings("unchecked")
  static <T> T genericMock(Class<? super T> classToMock) {
    return (T) Mockito.mock(classToMock);
  }
}
