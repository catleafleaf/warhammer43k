package combat.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 攻防计算公式工具类
 * 基于装甲优先抗伤理念，模拟防御力优先吸收伤害的战斗系统
 */
public class DefenseFirstCombatCalculator {

    // 系统常数
    private static final BigDecimal MAX_DAMAGE_REDUCTION = BigDecimal.valueOf(0.85); // 最大减伤85%
    private static final BigDecimal MIN_DEFENSE_STRENGTH = BigDecimal.valueOf(50);   // 最小防御强度
    private static final BigDecimal MIN_DEFENSE_PERCENT = BigDecimal.valueOf(0.05);  // 最小防御百分比5%

    /**
     * 计算攻防结果
     * 防御力优先吸收伤害，击穿后转为生命值伤害
     *
     * @param attackPower 攻击方的攻击力
     * @param defense 防御方的防御力
     * @param penetration 攻击方的穿透力 (0-1)
     * @param resistance 防御方的抗性 (0-1)
     * @param damageMultiplier 伤害倍率
     * @param extraPenetration 额外计算穿透
     * @param extraDefense 额外计算防御
     * @return 攻防计算结果
     */
    public static CombatResult calculateCombat(BigDecimal attackPower,       // 攻击力
                                               BigDecimal defense,           // 防御力
                                               BigDecimal penetration,       // 穿透力 (0-1)
                                               BigDecimal resistance,        // 抗性 (0-1)
                                               BigDecimal damageMultiplier,  // 伤害倍率
                                               BigDecimal extraPenetration,  // 额外计算穿透
                                               BigDecimal extraDefense) {    // 额外计算防御

        // 1. 计算穿透强度 = 攻击力 * 伤害倍率
        BigDecimal penetrationStrength = calculatePenetrationStrength(
                attackPower, damageMultiplier, penetration, resistance);

        // 2. 计算防御强度 = 防御力 * (1 + 额外防御) + 额外计算防御
        BigDecimal defenseStrength = calculateDefenseStrength(defense, extraDefense);

        // 3. 计算伤害系数 = 穿透强度 * (1 + 额外穿透) / (穿透强度 + 防御强度)
        BigDecimal damageFactor = calculateDamageFactor(penetrationStrength, defenseStrength, extraPenetration);

        // 4. 计算防御伤害强度 = 穿透强度 * 伤害系数
        BigDecimal defenseDamageStrength = penetrationStrength.multiply(damageFactor);

        // 5. 计算最小防御伤害 = 穿透强度 * (1 - 最大减伤)
        BigDecimal minDefenseDamage = penetrationStrength.multiply(BigDecimal.ONE.subtract(MAX_DAMAGE_REDUCTION));

        // 6. 计算实际防御伤害
        BigDecimal actualDefenseDamage = defenseDamageStrength.max(minDefenseDamage);

        // 7. 计算生命值伤害（击穿后的伤害）
        BigDecimal healthDamage = calculateHealthDamage(
                defenseDamageStrength, actualDefenseDamage, damageMultiplier, resistance);

        return new CombatResult(actualDefenseDamage, healthDamage, damageFactor,
                defenseDamageStrength.compareTo(actualDefenseDamage) > 0);
    }

    /**
     * 计算穿透强度 = 攻击力 * 伤害倍率
     * 模拟装甲系统中的"穿甲强度"
     */
    private static BigDecimal calculatePenetrationStrength(BigDecimal attackPower,
                                                           BigDecimal damageMultiplier,
                                                           BigDecimal penetration,
                                                           BigDecimal resistance) {
        // 等效攻击力 = 攻击力 * (1 + 穿透力) * (1 - 抗性)
        BigDecimal equivalentAttack = attackPower
                .multiply(BigDecimal.ONE.add(penetration))
                .multiply(BigDecimal.ONE.subtract(resistance));

        // 伤害倍率调整 = 伤害倍率 * (1 - 抗性)
        BigDecimal adjustedMultiplier = damageMultiplier.multiply(BigDecimal.ONE.subtract(resistance));

        return equivalentAttack.multiply(adjustedMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算防御强度 = 防御力 * (1 + 额外防御百分比) + 额外计算防御
     * 模拟装甲系统中的"装甲强度"
     */
    private static BigDecimal calculateDefenseStrength(BigDecimal defense, BigDecimal extraDefense) {
        BigDecimal baseDefenseStrength = defense.multiply(BigDecimal.ONE.add(extraDefense));

        // 最小防御强度 = max(5%防御力, 50)
        BigDecimal minDefense = defense.multiply(MIN_DEFENSE_PERCENT).max(MIN_DEFENSE_STRENGTH);

        return baseDefenseStrength.max(minDefense).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算伤害系数 = 穿透强度 * (1 + 额外穿透) / (穿透强度 + 防御强度)
     * 具有边际效应：防御力越高，每点防御提供的减伤越少
     */
    private static BigDecimal calculateDamageFactor(BigDecimal penetrationStrength,
                                                    BigDecimal defenseStrength,
                                                    BigDecimal extraPenetration) {
        if (defenseStrength.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE; // 没有防御，全额伤害
        }

        BigDecimal numerator = penetrationStrength.multiply(BigDecimal.ONE.add(extraPenetration));
        BigDecimal denominator = penetrationStrength.add(defenseStrength);

        return numerator.divide(denominator, 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE) // 伤害系数不超过100%
                .max(BigDecimal.ZERO); // 伤害系数不低于0%
    }

    /**
     * 计算生命值伤害 = (防御伤害强度 - 实际防御伤害) / 伤害倍率 * (1 - 抗性)
     * 模拟装甲系统中的"结构伤害"
     */
    private static BigDecimal calculateHealthDamage(BigDecimal defenseDamageStrength,
                                                    BigDecimal actualDefenseDamage,
                                                    BigDecimal damageMultiplier,
                                                    BigDecimal resistance) {
        // 溢出伤害 = 防御伤害强度 - 实际防御伤害
        BigDecimal overflowDamage = defenseDamageStrength.subtract(actualDefenseDamage);

        if (overflowDamage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO; // 没有溢出伤害
        }

        // 生命值伤害 = 溢出伤害 / 伤害倍率 * (1 - 抗性)
        BigDecimal healthDamage = overflowDamage
                .divide(damageMultiplier, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.ONE.subtract(resistance));

        return healthDamage.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算攻击有效性（用于AI和战斗评估）
     */
    public static BigDecimal calculateAttackEffectiveness(BigDecimal attackPower, BigDecimal defense) {
        if (defense.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }

        // 攻击有效性 = 攻击力 / (攻击力 + 防御力)
        return attackPower.divide(attackPower.add(defense), 4, RoundingMode.HALF_UP);
    }
}