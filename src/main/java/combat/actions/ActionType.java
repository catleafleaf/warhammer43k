package combat.actions;

public enum ActionType {
    DEPLOY("部署单位"),
    MOVE("移动单位"),
    ATTACK("攻击"),
    HEAL("治疗"),
    USE_RESOURCE("使用物资"),
    SPECIAL("特殊行动"),
    PASS("跳过回合");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}