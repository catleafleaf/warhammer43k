package combat.model;

/**
 * 战斗射程范围类
 */
public class CombatRange {
    private final int minRange; // 最小射程
    private final int maxRange; // 最大射程

    public CombatRange(int minRange, int maxRange) {
        this.minRange = Math.max(0, minRange);
        this.maxRange = Math.max(this.minRange, maxRange);
    }

    /**
     * 检查目标是否在射程范围内
     */
    public boolean isInRange(int distance) {
        return distance >= minRange && distance <= maxRange;
    }

    public int getMinRange() { return minRange; }
    public int getMaxRange() { return maxRange; }
}