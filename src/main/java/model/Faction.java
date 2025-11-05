package model;

/**
 * 战锤40k阵营枚举
 */
public enum Faction {
    IMPERIUM("人类帝国", "为了帝皇！"),
    CHAOS("混沌势力", "血祭血神！"),
    ELDAR("灵族", "古灵族的荣耀"),
    ORK("欧克蛮人", "WAAAGH！"),
    TYRANID("泰伦虫族", "生物质渴望"),
    NECRON("太空死灵", "苏醒的古老威胁"),
    TAU("钛帝国", "上上善道"),
    NEWORDER("新秩序","为了帝皇！！"),
    OTHER("其他", "未知势力");

    private final String displayName;
    private final String battleCry;

    Faction(String displayName, String battleCry) {
        this.displayName = displayName;
        this.battleCry = battleCry;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBattleCry() {
        return battleCry;
    }

    public static Faction fromDisplayName(String displayName) {
        for (Faction faction : values()) {
            if (faction.displayName.equals(displayName)) {
                return faction;
            }
        }
        return OTHER;
    }
}