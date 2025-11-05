package model;

public enum CampaignStatus {
    CREATED("已创建"),
    STARTED("已开始"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String displayName;

    CampaignStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}