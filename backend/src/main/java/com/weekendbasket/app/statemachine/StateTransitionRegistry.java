package com.weekendbasket.app.statemachine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class StateTransitionRegistry {

    private static final Logger log = LogManager.getLogger(StateTransitionRegistry.class);

    /**
     * Loads all JSON files under classpath:states/ at startup.
     * Returns a list of lists — each inner list is all transitions from one JSON file.
     * The worker searches across all inner lists to find the matching candidate.
     */
    @Bean
    public List<List<StateTransition>> allStateTransitions(ObjectMapper objectMapper) throws Exception {
        List<List<StateTransition>> allTransitions = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:states/*.json");

        for (Resource resource : resources) {
            List<StateTransition> transitions = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<>() {}
            );
            allTransitions.add(transitions);
            log.info("Loaded {} state transitions from {}", transitions.size(), resource.getFilename());
        }
        return allTransitions;
    }
}
