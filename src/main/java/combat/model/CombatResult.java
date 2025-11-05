package combat.model;

import java.math.BigDecimal;

/**
 * 攻防计算结果封装类
 * 包含防御伤害和生命值伤害的详细信息
 */
public class CombatResult {
    private final BigDecimal defenseDamage;         // 对防御力造成的伤害
    private final BigDecimal healthDamage;          // 对生命值造成的伤害（击穿后）
    private final BigDecimal damageFactor;          // 伤害系数
    private final boolean defensePenetrated;        // 是否击穿防御
    private final BigDecimal totalDamage;           // 总伤害

    public CombatResult(BigDecimal defenseDamage, BigDecimal healthDamage,
                        BigDecimal damageFactor, boolean defensePenetrated) {
        this.defenseDamage = defenseDamage;
        this.healthDamage = healthDamage;
        this.damageFactor = damageFactor;
        this.defensePenetrated = defensePenetrated;
        this.totalDamage = defenseDamage.add(healthDamage);
    }

    // Getter方法
    public BigDecimal getDefenseDamage() { return defenseDamage; }
    public BigDecimal getHealthDamage() { return healthDamage; }
    public BigDecimal getDamageFactor() { return damageFactor; }
    public boolean isDefensePenetrated() { return defensePenetrated; }
    public BigDecimal getTotalDamage() { return totalDamage; }

    /**
     * 获取详细战斗报告
     */
    public String getCombatReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 攻防计算结果 ===\n");
        report.append(String.format("伤害系数: %.2f%%\n", damageFactor.multiply(BigDecimal.valueOf(100))));
        report.append(String.format("防御伤害: %.2f\n", defenseDamage));

        if (defensePenetrated) {
            report.append(String.format("生命伤害: %.2f (击穿!)\n", healthDamage));
        } else {
            report.append("防御未被击穿\n");
        }

        report.append(String.format("总伤害: %.2f\n", totalDamage));
        return report.toString();
    }

    /**
     * 获取简化战斗信息（用于UI显示）
     */
    public String getSimpleCombatInfo() {
        if (defensePenetrated) {
            return String.format("造成 %.1f 伤害 (击穿)", totalDamage);
        } else {
            return String.format("造成 %.1f 伤害", totalDamage);
        }
    }
}