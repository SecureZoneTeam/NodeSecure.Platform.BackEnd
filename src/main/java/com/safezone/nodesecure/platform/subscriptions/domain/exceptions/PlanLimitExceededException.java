package com.safezone.nodesecure.platform.subscriptions.domain.exceptions;

import com.safezone.nodesecure.platform.shared.domain.exceptions.BusinessRuleViolationException;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.LimitViolation;

import java.util.List;

/**
 * The plan of the organization does not allow the operation. Answered as HTTP 409 with a
 * structured body (code, plan and the affected resources with their usage and limit) so the
 * client can explain the problem and offer a plan change.
 */
public class PlanLimitExceededException extends BusinessRuleViolationException {

    /** Creating one more resource would exceed the active plan. */
    public static final String LIMIT_REACHED = "PLAN_LIMIT_REACHED";
    /** The current usage does not fit in the plan being activated or changed to. */
    public static final String USAGE_EXCEEDS_PLAN = "PLAN_USAGE_EXCEEDS_LIMITS";

    private final String code;
    private final String planCode;
    private final String planName;
    private final transient List<LimitViolation> violations;

    private PlanLimitExceededException(String code, String messageKey, String planCode, String planName,
                                       List<LimitViolation> violations, Object... arguments) {
        super(messageKey, arguments);
        this.code = code;
        this.planCode = planCode;
        this.planName = planName;
        this.violations = List.copyOf(violations);
    }

    public static PlanLimitExceededException limitReached(String planCode, String planName, LimitViolation violation) {
        return new PlanLimitExceededException(LIMIT_REACHED,
                "subscriptions.error.limitReached." + violation.resource().key(),
                planCode, planName, List.of(violation), planName, violation.used(), violation.limit());
    }

    public static PlanLimitExceededException usageExceedsPlan(String planCode, String planName,
                                                              List<LimitViolation> violations) {
        String detail = String.join(", ", violations.stream().map(LimitViolation::toString).toList());
        return new PlanLimitExceededException(USAGE_EXCEEDS_PLAN, "subscriptions.error.limitsExceeded",
                planCode, planName, violations, planName, detail);
    }

    public String getCode() {
        return code;
    }

    public String getPlanCode() {
        return planCode;
    }

    public String getPlanName() {
        return planName;
    }

    public List<LimitViolation> getViolations() {
        return violations;
    }
}
