package combat.model;

public enum TargetingType {
    NONE("无目标"),
    SINGLE("单体目标"),
    MULTIPLE("多个目标"),
    AREA("区域目标"),
    SELF("自身目标");

    private final String description;

    TargetingType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}