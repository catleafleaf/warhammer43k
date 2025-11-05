package combat.model;

public enum CombatStatus {
    NORMAL("正常"),
    STUNNED("眩晕"),
    SLOWED("减速"),
    POISONED("中毒"),
    BLEEDING("流血"),
    INSPIRED("激励"),
    FORTIFIED("加固");

    private final String description;

    CombatStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}