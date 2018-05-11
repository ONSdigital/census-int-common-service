package uk.gov.ons.ctp.common.state;

import lombok.Data;
import lombok.Getter;
import uk.gov.ons.ctp.common.error.CTPException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A Simple impl of StateTransitionManager
 *
 * @param <S> The state type we transit from and to
 * @param <E> The event type that effects the transition
 */
@Data
@Getter
public class BasicStateTransitionManager<S, E> implements StateTransitionManager<S, E> {

  public static final String TRANSITION_ERROR_MSG = "State Transition from %s via %s is forbidden.";

  private Map<S, Map<E, S>> transitions = new HashMap<>();

  /**
   * Construct the instance with a provided map of transitions
   * @param transitionMap the transitions
   */
  public BasicStateTransitionManager(final Map<S, Map<E, S>> transitionMap) {
    transitions = transitionMap;
  }

  @Override
  public S transition(final S sourceState, final E event) throws CTPException {
    S destinationState = null;
    Map<E, S> outputMap = transitions.get(sourceState);
    if (outputMap != null) {
      destinationState = outputMap.get(event);
      log.info("Moving from " + sourceState.getClass().getName() + " to " + destinationState.getClass().getName() + " due to " + event.getClass().getName());
    }
    if (destinationState == null) {
    	log.error("No valid transition found from " + sourceState.getClass().getName() + " using " + event.getClass().getName());
      throw new CTPException(CTPException.Fault.BAD_REQUEST, String.format(TRANSITION_ERROR_MSG, sourceState, event));
    }
    return destinationState;
  }

  @Override
  public Map<E, S> getAvailableTransitions(final S sourceState) {
    Map<E, S> outputMap = transitions.get(sourceState);
    if (outputMap != null) {
      return outputMap;
    }
    return Collections.emptyMap();
  }

}
