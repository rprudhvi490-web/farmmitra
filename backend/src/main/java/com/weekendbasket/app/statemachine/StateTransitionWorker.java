package com.weekendbasket.app.statemachine;

import com.weekendbasket.app.exception.WeekendBasketException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StateTransitionWorker {

    private final List<List<StateTransition>> allStateTransitions;

    /**
     * Full flow:
     * 1. Search all inner lists to find a candidate matching entity + currentState + action
     * 2. Validate that currentState matches candidate.from
     * 3. Validate that targetState is in candidate.to
     * 4. Return the matched transition (caller applies the state change + sideEffect)
     */
    public StateTransition apply(String entity, String currentState, String targetState, String action) {
        // Step 1 — find candidate across all inner lists
        Optional<StateTransition> candidate = allStateTransitions.stream()
                .flatMap(List::stream)
                .filter(t -> t.getEntity().equalsIgnoreCase(entity)
                        && t.getAction().equalsIgnoreCase(action)
                        && t.getFrom().equalsIgnoreCase(currentState))
                .findFirst();

        if (candidate.isEmpty()) {
            throw new WeekendBasketException(
                    String.format("No transition defined for entity=%s, action=%s, from=%s",
                            entity, action, currentState));
        }

        StateTransition transition = candidate.get();

        // Step 2 — validate current state matches candidate.from
        if (!transition.getFrom().equalsIgnoreCase(currentState)) {
            throw new WeekendBasketException(
                    String.format("Current state mismatch. Expected=%s, Actual=%s",
                            transition.getFrom(), currentState));
        }

        // Step 3 — validate target state is in candidate.to
        boolean validTarget = transition.getTo().stream()
                .anyMatch(s -> s.equalsIgnoreCase(targetState));

        if (!validTarget) {
            throw new WeekendBasketException(
                    String.format("Invalid transition for %s: %s → %s (allowed: %s)",
                            entity, currentState, targetState, transition.getTo()));
        }

        return transition;
    }
}
