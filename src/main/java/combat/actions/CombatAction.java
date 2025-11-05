package combat.actions;

import combat.model.CombatUnit;

/**
 * 战斗动作抽象基类
 */
public abstract class CombatAction {
    protected final String actionId;
    protected final String name;
    protected final CombatUnit sourceUnit;
    protected ActionType actionType;
    protected boolean executed;

    public CombatAction(String name, CombatUnit sourceUnit, ActionType actionType) {
        this.actionId = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.sourceUnit = sourceUnit;
        this.actionType = actionType;
        this.executed = false;
    }

    public abstract ValidationResult validate();
    public abstract ActionResult execute();
    public abstract String getDescription();

    public String getActionId() { return actionId; }
    public String getName() { return name; }
    public CombatUnit getSourceUnit() { return sourceUnit; }
    public ActionType getActionType() { return actionType; }
    public boolean isExecuted() { return executed; }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }

        public static ValidationResult success() {
            return new ValidationResult(true, "验证通过");
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }

    public static class ActionResult {
        private final boolean success;
        private final String message;
        private final Object data;

        public ActionResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }

        public static ActionResult success(String message) {
            return new ActionResult(true, message, null);
        }

        public static ActionResult success(String message, Object data) {
            return new ActionResult(true, message, data);
        }

        public static ActionResult failure(String message) {
            return new ActionResult(false, message, null);
        }
    }
}