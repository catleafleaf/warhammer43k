package combat.model;

public enum ResourceType {
    DAMAGE("伤害"),
    HEAL("治疗"),
    CONTROL("控制"),
    BUFF("增益"),
    DEBUFF("减益"),
    AOE_DAMAGE("范围伤害"),
    AOE_HEAL("范围治疗"),
    UTILITY("功能");

    private final String description;

    ResourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}