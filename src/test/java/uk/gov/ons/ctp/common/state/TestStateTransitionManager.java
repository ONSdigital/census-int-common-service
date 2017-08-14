package uk.gov.ons.ctp.common.state;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import uk.gov.ons.ctp.common.error.CTPException;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static uk.gov.ons.ctp.common.state.BasicStateTransitionManager.TRANSITION_ERROR_MSG;

/**
 * A test of the state transition manager
 * It simply has to test a single good and a single bad transition - all it is testing is the underlying mechanism,
 * not a real implementation, where we will want to assert all of the valid and invalid transitions
 *
 */
public class TestStateTransitionManager {

  private StateTransitionManager<TestState, TestEvent> stm;

  /**
   * Setup the transitions
   */
  @Before
  public void setUp() {
    Map<TestState, Map<TestEvent, TestState>> transitions = new HashMap<>();
    Map<TestEvent, TestState> transitionMapForSubmitted = new HashMap<>();
    transitionMapForSubmitted.put(TestEvent.REQUEST_DISTRIBUTED, TestState.PENDING);
    transitions.put(TestState.SUBMITTED, transitionMapForSubmitted);
    stm = new BasicStateTransitionManager<>(transitions);
  }

  /**
   * test a valid transition
   * @throws CTPException if transition does
   */
  @Test
  public void testGood() throws CTPException {
   assertEquals(TestState.PENDING, stm.transition(TestState.SUBMITTED, TestEvent.REQUEST_DISTRIBUTED));
  }

  /**
   * tests a bad transition
   */
  @Test
  public void testBad() {
   try {
     stm.transition(TestState.SUBMITTED, TestEvent.REQUEST_ACCEPTED);
     fail();
   } catch (CTPException e) {
     assertEquals(CTPException.Fault.BAD_REQUEST, e.getFault());
     assertEquals(String.format(TRANSITION_ERROR_MSG, TestState.SUBMITTED, TestEvent.REQUEST_ACCEPTED), e.getMessage());
   }
  }
}
