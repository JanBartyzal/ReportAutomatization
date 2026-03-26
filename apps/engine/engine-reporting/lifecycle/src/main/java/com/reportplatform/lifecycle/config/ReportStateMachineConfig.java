package com.reportplatform.lifecycle.config;

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
import java.util.Set;

/**
 * Spring State Machine configuration for report lifecycle transitions.
 *
 * Central flow: DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED
 *                                                ↘ REJECTED → DRAFT (resubmit)
 * Local flow:   DRAFT → COMPLETED (no holding approval)
 *               COMPLETED → SUBMITTED (optional release to central flow)
 */
@Configuration
@EnableStateMachineFactory
public class ReportStateMachineConfig extends EnumStateMachineConfigurerAdapter<ReportState, ReportEvent> {

    private static final Logger log = LoggerFactory.getLogger(ReportStateMachineConfig.class);

    public static final String HEADER_USER_ID = "userId";
    public static final String HEADER_USER_ROLE = "userRole";
    public static final String HEADER_REPORT_ID = "reportId";
    public static final String HEADER_COMMENT = "comment";
    public static final String HEADER_SCOPE = "scope";

    private static final Set<String> SUBMIT_ROLES = Set.of("EDITOR", "HOLDING_ADMIN");
    private static final Set<String> REVIEW_ROLES = Set.of("REVIEWER", "HOLDING_ADMIN");
    private static final Set<String> LOCAL_COMPLETE_ROLES = Set.of("COMPANY_ADMIN", "EDITOR", "HOLDING_ADMIN");

    @Override
    public void configure(StateMachineConfigurationConfigurer<ReportState, ReportEvent> config) throws Exception {
        config
                .withConfiguration()
                .autoStartup(false)
                .listener(new ReportStateMachineListener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<ReportState, ReportEvent> states) throws Exception {
        states
                .withStates()
                .initial(ReportState.DRAFT)
                .end(ReportState.APPROVED)
                .end(ReportState.COMPLETED)
                .states(EnumSet.allOf(ReportState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ReportState, ReportEvent> transitions) throws Exception {
        transitions
                // DRAFT → SUBMITTED (Editor submits)
                .withExternal()
                    .source(ReportState.DRAFT).target(ReportState.SUBMITTED)
                    .event(ReportEvent.SUBMIT)
                    .guard(editorRoleGuard())
                    .action(logTransitionAction())
                    .and()
                // SUBMITTED → UNDER_REVIEW (Reviewer starts review)
                .withExternal()
                    .source(ReportState.SUBMITTED).target(ReportState.UNDER_REVIEW)
                    .event(ReportEvent.START_REVIEW)
                    .guard(reviewerRoleGuard())
                    .action(logTransitionAction())
                    .and()
                // UNDER_REVIEW → APPROVED (Reviewer approves)
                .withExternal()
                    .source(ReportState.UNDER_REVIEW).target(ReportState.APPROVED)
                    .event(ReportEvent.APPROVE)
                    .guard(reviewerRoleGuard())
                    .action(logTransitionAction())
                    .and()
                // UNDER_REVIEW → REJECTED (Reviewer rejects, comment mandatory)
                .withExternal()
                    .source(ReportState.UNDER_REVIEW).target(ReportState.REJECTED)
                    .event(ReportEvent.REJECT)
                    .guard(rejectGuard())
                    .action(logTransitionAction())
                    .and()
                // SUBMITTED → REJECTED (Admin can reject without formal review)
                .withExternal()
                    .source(ReportState.SUBMITTED).target(ReportState.REJECTED)
                    .event(ReportEvent.REJECT)
                    .action(logTransitionAction())
                    .and()
                // REJECTED → DRAFT (Editor resubmits after correction)
                .withExternal()
                    .source(ReportState.REJECTED).target(ReportState.DRAFT)
                    .event(ReportEvent.RESUBMIT)
                    .guard(editorRoleGuard())
                    .action(logTransitionAction())
                    .and()
                // DRAFT → COMPLETED (CompanyAdmin completes local report, no holding approval)
                .withExternal()
                    .source(ReportState.DRAFT).target(ReportState.COMPLETED)
                    .event(ReportEvent.COMPLETE)
                    .guard(localScopeGuard())
                    .guard(companyAdminRoleGuard())
                    .action(logTransitionAction())
                    .and()
                // COMPLETED → SUBMITTED (Release local report to central approval flow)
                .withExternal()
                    .source(ReportState.COMPLETED).target(ReportState.SUBMITTED)
                    .event(ReportEvent.RELEASE)
                    .guard(companyAdminRoleGuard())
                    .action(logTransitionAction());
    }

    @Bean
    public Guard<ReportState, ReportEvent> editorRoleGuard() {
        return context -> {
            String role = context.getMessageHeaders().get(HEADER_USER_ROLE, String.class);
            if (role == null || !SUBMIT_ROLES.contains(role)) {
                log.warn("Guard rejected: user role '{}' cannot submit/resubmit reports", role);
                return false;
            }
            return true;
        };
    }

    @Bean
    public Guard<ReportState, ReportEvent> reviewerRoleGuard() {
        return context -> {
            String role = context.getMessageHeaders().get(HEADER_USER_ROLE, String.class);
            if (role == null || !REVIEW_ROLES.contains(role)) {
                log.warn("Guard rejected: user role '{}' cannot review/approve reports", role);
                return false;
            }
            return true;
        };
    }

    @Bean
    public Guard<ReportState, ReportEvent> rejectGuard() {
        return context -> {
            String role = context.getMessageHeaders().get(HEADER_USER_ROLE, String.class);
            if (role == null || !REVIEW_ROLES.contains(role)) {
                log.warn("Guard rejected: user role '{}' cannot reject reports", role);
                return false;
            }
            String comment = context.getMessageHeaders().get(HEADER_COMMENT, String.class);
            if (comment == null || comment.isBlank()) {
                log.warn("Guard rejected: rejection comment is mandatory");
                return false;
            }
            return true;
        };
    }

    @Bean
    public Guard<ReportState, ReportEvent> localScopeGuard() {
        return context -> {
            String scope = context.getMessageHeaders().get(HEADER_SCOPE, String.class);
            if (!"LOCAL".equals(scope)) {
                log.warn("Guard rejected: COMPLETE transition only allowed for LOCAL scope, got '{}'", scope);
                return false;
            }
            return true;
        };
    }

    @Bean
    public Guard<ReportState, ReportEvent> companyAdminRoleGuard() {
        return context -> {
            String role = context.getMessageHeaders().get(HEADER_USER_ROLE, String.class);
            if (role == null || !LOCAL_COMPLETE_ROLES.contains(role)) {
                log.warn("Guard rejected: user role '{}' cannot complete/release local reports", role);
                return false;
            }
            return true;
        };
    }

    @Bean
    public Action<ReportState, ReportEvent> logTransitionAction() {
        return context -> {
            String reportId = context.getMessageHeaders().get(HEADER_REPORT_ID, String.class);
            String userId = context.getMessageHeaders().get(HEADER_USER_ID, String.class);
            log.info("Report [{}] by user [{}]: {} -> {}",
                    reportId, userId,
                    context.getSource().getId(),
                    context.getTarget().getId());
        };
    }

    private static class ReportStateMachineListener
            extends StateMachineListenerAdapter<ReportState, ReportEvent> {

        @Override
        public void stateChanged(State<ReportState, ReportEvent> from,
                                 State<ReportState, ReportEvent> to) {
            LoggerFactory.getLogger(ReportStateMachineListener.class)
                    .debug("Report state machine transition: {} -> {}",
                            from != null ? from.getId() : "NONE",
                            to != null ? to.getId() : "NONE");
        }
    }
}
