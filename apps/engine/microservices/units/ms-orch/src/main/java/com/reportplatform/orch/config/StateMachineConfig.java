package com.reportplatform.orch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

/**
 * Configures the Spring State Machine for file processing workflows.
 * <p>
 * The workflow follows a linear pipeline: RECEIVED -> SCANNING -> PARSING -> MAPPING -> STORING -> COMPLETED,
 * with any state able to transition to FAILED on error.
 * </p>
 */
@Configuration
@EnableStateMachineFactory
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<WorkflowState, WorkflowEvent> {

    private static final Logger log = LoggerFactory.getLogger(StateMachineConfig.class);

    private static final String FILE_ID_HEADER = "fileId";
    private static final String WORKFLOW_ID_HEADER = "workflowId";
    private static final String ERROR_DETAIL_HEADER = "errorDetail";

    @Override
    public void configure(StateMachineConfigurationConfigurer<WorkflowState, WorkflowEvent> config) throws Exception {
        config
                .withConfiguration()
                .autoStartup(false)
                .listener(new WorkflowStateMachineListener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<WorkflowState, WorkflowEvent> states) throws Exception {
        states
                .withStates()
                .initial(WorkflowState.RECEIVED)
                .end(WorkflowState.COMPLETED)
                .end(WorkflowState.FAILED)
                .states(EnumSet.allOf(WorkflowState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<WorkflowState, WorkflowEvent> transitions) throws Exception {
        transitions
                // Happy path
                .withExternal()
                    .source(WorkflowState.RECEIVED).target(WorkflowState.SCANNING)
                    .event(WorkflowEvent.FILE_RECEIVED)
                    .guard(fileIdPresentGuard())
                    .action(logTransitionAction())
                    .and()
                .withExternal()
                    .source(WorkflowState.SCANNING).target(WorkflowState.PARSING)
                    .event(WorkflowEvent.SCAN_COMPLETE)
                    .guard(scanCleanGuard())
                    .action(logTransitionAction())
                    .and()
                .withExternal()
                    .source(WorkflowState.PARSING).target(WorkflowState.MAPPING)
                    .event(WorkflowEvent.PARSE_COMPLETE)
                    .action(logTransitionAction())
                    .and()
                .withExternal()
                    .source(WorkflowState.MAPPING).target(WorkflowState.STORING)
                    .event(WorkflowEvent.MAP_COMPLETE)
                    .action(logTransitionAction())
                    .and()
                .withExternal()
                    .source(WorkflowState.STORING).target(WorkflowState.COMPLETED)
                    .event(WorkflowEvent.STORE_COMPLETE)
                    .action(logTransitionAction())
                    .and()

                // Error transitions from any processing state
                .withExternal()
                    .source(WorkflowState.SCANNING).target(WorkflowState.FAILED)
                    .event(WorkflowEvent.ERROR)
                    .action(errorAction())
                    .and()
                .withExternal()
                    .source(WorkflowState.PARSING).target(WorkflowState.FAILED)
                    .event(WorkflowEvent.ERROR)
                    .action(errorAction())
                    .and()
                .withExternal()
                    .source(WorkflowState.MAPPING).target(WorkflowState.FAILED)
                    .event(WorkflowEvent.ERROR)
                    .action(errorAction())
                    .and()
                .withExternal()
                    .source(WorkflowState.STORING).target(WorkflowState.FAILED)
                    .event(WorkflowEvent.ERROR)
                    .action(errorAction());
    }

    @Bean
    public Guard<WorkflowState, WorkflowEvent> fileIdPresentGuard() {
        return context -> {
            String fileId = context.getMessageHeaders().get(FILE_ID_HEADER, String.class);
            if (fileId == null || fileId.isBlank()) {
                log.warn("Guard rejected transition: missing fileId header");
                return false;
            }
            return true;
        };
    }

    @Bean
    public Guard<WorkflowState, WorkflowEvent> scanCleanGuard() {
        return context -> {
            Boolean virusDetected = context.getExtendedState()
                    .get("virusDetected", Boolean.class);
            if (Boolean.TRUE.equals(virusDetected)) {
                log.warn("Guard rejected transition: virus detected in file {}",
                        context.getMessageHeaders().get(FILE_ID_HEADER));
                return false;
            }
            return true;
        };
    }

    @Bean
    public Action<WorkflowState, WorkflowEvent> logTransitionAction() {
        return context -> {
            String fileId = context.getMessageHeaders().get(FILE_ID_HEADER, String.class);
            String workflowId = context.getMessageHeaders().get(WORKFLOW_ID_HEADER, String.class);
            log.info("Workflow [{}] file [{}]: {} -> {}",
                    workflowId, fileId,
                    context.getSource().getId(),
                    context.getTarget().getId());
        };
    }

    @Bean
    public Action<WorkflowState, WorkflowEvent> errorAction() {
        return context -> {
            String fileId = context.getMessageHeaders().get(FILE_ID_HEADER, String.class);
            String workflowId = context.getMessageHeaders().get(WORKFLOW_ID_HEADER, String.class);
            String errorDetail = context.getMessageHeaders().get(ERROR_DETAIL_HEADER, String.class);
            log.error("Workflow [{}] file [{}] failed at state {}: {}",
                    workflowId, fileId,
                    context.getSource().getId(),
                    errorDetail);
        };
    }

    private static class WorkflowStateMachineListener
            extends StateMachineListenerAdapter<WorkflowState, WorkflowEvent> {

        @Override
        public void stateChanged(State<WorkflowState, WorkflowEvent> from,
                                 State<WorkflowState, WorkflowEvent> to) {
            LoggerFactory.getLogger(WorkflowStateMachineListener.class)
                    .debug("State machine transition: {} -> {}",
                            from != null ? from.getId() : "NONE",
                            to != null ? to.getId() : "NONE");
        }
    }
}
