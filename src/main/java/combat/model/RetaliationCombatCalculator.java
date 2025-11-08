package combat.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 带反击机制的战斗计算器
 */
public class RetaliationCombatCalculator {
    private static final BigDecimal INITIAL_DAMAGE_RATIO = new BigDecimal("0.10"); // 初始伤害比例10%
    private static final BigDecimal RETALIATION_DAMAGE_RATIO = new BigDecimal("0.85"); // 反击伤害比例85%

    /**
     * 计算包含反击的战斗结果
     */
    public static RetaliationResult calculateCombat(CombatUnit attacker, CombatUnit defender) {
        // 获取双方距离
        int distance = attacker.getPosition().distanceTo(defender.getPosition());

        // 检查攻击者是否在射程内
        if (!attacker.getCombatRange().isInRange(distance)) {
            return RetaliationResult.outOfRange();
        }

        // 计算初始伤害（10%）
        CombatResult initialDamage = calculateInitialDamage(attacker, defender);

        // 检查被攻击者是否可以反击（是否在射程内）
        boolean canRetaliate = defender.getCombatRange().isInRange(distance);

        if (!canRetaliate) {
            // 如果不能反击，结算全部伤害
            CombatResult fullDamage = calculateRemainingDamage(attacker, defender, initialDamage);
            return new RetaliationResult(fullDamage, null);
        } else {
            // 如果可以反击，同时结算剩余伤害和反击伤害
            CombatResult remainingDamage = calculateRemainingDamage(attacker, defender, initialDamage);
            CombatResult retaliationDamage = calculateRetaliationDamage(defender, attacker);
            return new RetaliationResult(remainingDamage, retaliationDamage);
        }
    }

    /**
     * 计算初始伤害（10%）
     */
    private static CombatResult calculateInitialDamage(CombatUnit attacker, CombatUnit defender) {
        BigDecimal initialAttack = attacker.getAttack().multiply(INITIAL_DAMAGE_RATIO)
                .setScale(2, RoundingMode.HALF_UP);

        return DefenseFirstCombatCalculator.calculateCombat(
                initialAttack,
                defender.getCurrentDefense(),
                attacker.getArmorPenetration(),
                defender.getResistance(),
                attacker.getDamageMultiplier(),
                attacker.getExtraPenetration(),
                defender.getExtraDefense()
        );
    }

    /**
     * 计算剩余伤害（90%）
     */
    private static CombatResult calculateRemainingDamage(CombatUnit attacker, CombatUnit defender,
                                                         CombatResult initialDamage) {
        BigDecimal remainingAttack = attacker.getAttack()
                .multiply(BigDecimal.ONE.subtract(INITIAL_DAMAGE_RATIO))
                .setScale(2, RoundingMode.HALF_UP);

        return DefenseFirstCombatCalculator.calculateCombat(
                remainingAttack,
                defender.getCurrentDefense().subtract(initialDamage.getDefenseDamage()),
                attacker.getArmorPenetration(),
                defender.getResistance(),
                attacker.getDamageMultiplier(),
                attacker.getExtraPenetration(),
                defender.getExtraDefense()
        );
    }

    /**
     * 计算反击伤害（85%攻击力）
     */
    private static CombatResult calculateRetaliationDamage(CombatUnit retaliator, CombatUnit target) {
        BigDecimal retaliationAttack = retaliator.getAttack()
                .multiply(RETALIATION_DAMAGE_RATIO)
                .setScale(2, RoundingMode.HALF_UP);

        return DefenseFirstCombatCalculator.calculateCombat(
                retaliationAttack,
                target.getCurrentDefense(),
                retaliator.getArmorPenetration(),
                target.getResistance(),
                retaliator.getDamageMultiplier(),
                retaliator.getExtraPenetration(),
                target.getExtraDefense()
        );
    }

    /**
     * 反击战斗结果类
     */
    public static class RetaliationResult {
        private final CombatResult attackerDamage; // 攻击者造成的伤害
        private final CombatResult retaliationDamage; // 反击造成的伤害
        private final boolean inRange; // 是否在射程内

        private RetaliationResult(CombatResult attackerDamage, CombatResult retaliationDamage) {
            this.attackerDamage = attackerDamage;
            this.retaliationDamage = retaliationDamage;
            this.inRange = true;
        }

        private RetaliationResult() {
            this.attackerDamage = null;
            this.retaliationDamage = null;
            this.inRange = false;
        }

        public static RetaliationResult outOfRange() {
            return new RetaliationResult();
        }

        public CombatResult getAttackerDamage() { return attackerDamage; }
        public CombatResult getRetaliationDamage() { return retaliationDamage; }
        public boolean isInRange() { return inRange; }
        public boolean hasRetaliation() { return retaliationDamage != null; }

        /**
         * 获取战斗报告
         */
        public String getCombatReport() {
            if (!inRange) {
                return "⚠️ 目标不在射程范围内";
            }

            StringBuilder report = new StringBuilder();
            report.append("=== 战斗结算 ===\n");

            // 初始伤害报告
            report.append("【初始伤害阶段】\n");
            report.append(attackerDamage.getCombatReport());

            // 反击伤害报告
            if (hasRetaliation()) {
                report.append("\n【反击阶段】\n");
                report.append(retaliationDamage.getCombatReport());
            }

            return report.toString();
        }
    }
}