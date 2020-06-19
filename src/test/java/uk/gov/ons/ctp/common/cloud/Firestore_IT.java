package uk.gov.ons.ctp.common.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * This class tests the FirestoreDataStore class by connecting to a real firestore project.
 *
 * <p>To run this code: This class tests Firestore using the firestore API. 1) Uncomment the @Ignore
 * annotations. 2) Make sure the Firestore environment variables are set. eg, I use:
 * GOOGLE_APPLICATION_CREDENTIALS =
 * /Users/peterbochel/.config/gcloud/application_default_credentials.json GOOGLE_CLOUD_PROJECT =
 * census-rh-peterb
 */
@Ignore
public class Firestore_IT extends CloudTestBase {
  private static final String FIRESTORE_PROJECT_ENV_NAME = "GOOGLE_CLOUD_PROJECT";
  private static FirestoreDataStore firestoreDataStore;

  @BeforeClass
  public static void setUp() {
    firestoreDataStore = new FirestoreDataStore();
    ReflectionTestUtils.setField(
        firestoreDataStore, "gcpProject", System.getenv(FIRESTORE_PROJECT_ENV_NAME));
    firestoreDataStore.create();
  }

  @Before
  public void clearTestData() throws Exception {
    // Add data to collection
    firestoreDataStore.deleteObject(TEST_SCHEMA, CASE1.getId());
    firestoreDataStore.deleteObject(TEST_SCHEMA, CASE2.getId());
  }

  @Test
  public void testStoreAndRetrieve() throws Exception {
    // Add data to collection
    firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);
    firestoreDataStore.storeObject(TEST_SCHEMA, CASE2.getId(), CASE2);

    verifyStoredCase(CASE1);
    verifyStoredCase(CASE2);
  }

  private void verifyStoredCase(DummyCase caze) throws Exception {
    Optional<DummyCase> retrievedCase =
        firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, caze.getId());
    assertTrue(retrievedCase.isPresent());
    DummyCase rcase = retrievedCase.get();
    assertEquals(caze, rcase);
  }

  @Test
  public void testReplaceObject() throws Exception {
    // Add data to collection
    String id = CASE1.getId();
    firestoreDataStore.storeObject(TEST_SCHEMA, id, CASE1);

    // Verify that 1st case can be read back
    Optional<DummyCase> retrievedCase =
        firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, id);
    assertEquals(CASE1, retrievedCase.get());

    // Replace contents with CASE2
    firestoreDataStore.storeObject(TEST_SCHEMA, id, CASE2);

    // Confirm contents of 'id', which was CASE1, is now CASE2
    retrievedCase = firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, id);
    assertEquals(CASE2, retrievedCase.get());
  }

  @Test
  public void testRetrieveObject_unknownObject() throws Exception {
    firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);

    // Verify that reading a non existent object returns null
    String unknownUUID = UUID.randomUUID().toString();
    Optional<DummyCase> retrievedCase =
        firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, unknownUUID);
    assertTrue(retrievedCase.isEmpty());
  }

  @Test
  public void testSearch_multipleResults() throws Exception {
    // Add data to collection
    firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);
    firestoreDataStore.storeObject(TEST_SCHEMA, CASE2.getId(), CASE2);

    // Verify that search can find the first case
    String[] searchByForename = new String[] {"contact", "forename"};
    List<DummyCase> retrievedCase1 =
        firestoreDataStore.search(
            DummyCase.class, TEST_SCHEMA, searchByForename, CASE1.getContact().getForename());
    assertEquals(1, retrievedCase1.size());
    assertEquals(CASE1.getId(), retrievedCase1.get(0).getId());
    assertEquals(CASE1, retrievedCase1.get(0));

    // Verify that search can find the second case
    String[] searchBySurname = new String[] {"contact", "surname"};
    List<DummyCase> retrievedCase2 =
        firestoreDataStore.search(
            DummyCase.class, TEST_SCHEMA, searchBySurname, CASE2.getContact().getSurname());
    assertEquals(2, retrievedCase2.size());
    assertEquals(CASE1, retrievedCase2.get(0));
    assertEquals(CASE2, retrievedCase2.get(1));
  }

  @Test
  public void testSearch_noResults() throws Exception {
    firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);

    // Verify that there are no results when searching for unknown forename
    String[] searchCriteria = new String[] {"contact", "forename"};
    List<DummyCase> retrievedCase1 =
        firestoreDataStore.search(DummyCase.class, TEST_SCHEMA, searchCriteria, "Bob");
    assertTrue(retrievedCase1.isEmpty());
  }

  @Test
  public void testSearch_serialisationFailure() throws Exception {
    firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);

    // Verify that search can find the first case
    boolean exceptionCaught = false;
    try {
      String[] searchByForename = new String[] {"contact", "forename"};
      firestoreDataStore.search(
          String.class, TEST_SCHEMA, searchByForename, CASE1.getContact().getForename());
    } catch (Exception e) {
      assertTrue(e.getCause().getMessage(), e.getCause().getMessage().contains("e"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testDelete_success() throws Exception {
    firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);

    // Verify that the case can be read back
    Optional<DummyCase> retrievedCase =
        firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, CASE1.getId());
    assertEquals(CASE1, retrievedCase.get());

    // Delete it
    firestoreDataStore.deleteObject(TEST_SCHEMA, CASE1.getId());

    // Now confirm that we can no longer read the deleted case
    retrievedCase = firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, CASE1.getId());
    assertTrue(retrievedCase.isEmpty());
  }

  @Test
  public void testDelete_onNonExistantObject() throws Exception {
    // Load test data, just so that Firestore has some data loaded
    firestoreDataStore.storeObject(TEST_SCHEMA, CASE1.getId(), CASE1);

    // Attempt to delete a non existent object. Should not fail
    UUID nonExistantUUID = UUID.randomUUID();
    firestoreDataStore.deleteObject(TEST_SCHEMA, nonExistantUUID.toString());

    // Confirm that firestore can't read the case
    Optional<DummyCase> retrievedCase =
        firestoreDataStore.retrieveObject(DummyCase.class, TEST_SCHEMA, nonExistantUUID.toString());
    assertTrue(retrievedCase.isEmpty());
  }
}
