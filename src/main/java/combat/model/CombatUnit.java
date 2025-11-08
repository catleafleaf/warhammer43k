package combat.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;


public class CombatUnit {
    private final String id;
    private String name;
    private String type;
    private BigDecimal attack;
    private BigDecimal defense;
    private BigDecimal maxHealth;
    private BigDecimal currentHealth;
    private BigDecimal armorPenetration;
    private BigDecimal resistance;
    private int movement;
    private BigDecimal control;
    private String ownerId;
    private Position position;
    private boolean isDeployed;
    private CombatStatus status;
    private boolean hasMovedThisTurn;
    private boolean hasActedThisTurn;
    private CombatRange combatRange;

    // 战斗属性
    private BigDecimal currentDefense;
    private final BigDecimal maxDefense;
    private BigDecimal damageMultiplier;
    private BigDecimal extraPenetration;
    private BigDecimal extraDefense;

    public CombatUnit(String name, String type, BigDecimal attack, BigDecimal defense,
                      BigDecimal health, BigDecimal armorPenetration, BigDecimal resistance,
                      int movement, BigDecimal control, String ownerId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.attack = setPrecision(attack);
        this.defense = setPrecision(defense);
        this.maxHealth = setPrecision(health);
        this.currentHealth = setPrecision(health);
        this.armorPenetration = setPrecision(armorPenetration);
        this.resistance = setPrecision(resistance);
        this.movement = movement;
        this.control = setPrecision(control);
        this.ownerId = ownerId;
        this.position = new Position(-1, -1);
        this.isDeployed = false;
        this.status = CombatStatus.NORMAL;
        this.hasMovedThisTurn = false;
        this.hasActedThisTurn = false;
        this.combatRange = new CombatRange(1, 1);

        // 初始化战斗属性
        this.currentDefense = setPrecision(defense);
        this.maxDefense = setPrecision(defense);
        this.damageMultiplier = BigDecimal.ONE;
        this.extraPenetration = BigDecimal.ZERO;
        this.extraDefense = BigDecimal.ZERO;
    }

    public CombatUnit(String name, String type, double attack, double defense,
                      double health, double armorPenetration, double resistance,
                      int movement, double control, String ownerId) {
        this(name, type,
                BigDecimal.valueOf(attack),
                BigDecimal.valueOf(defense),
                BigDecimal.valueOf(health),
                BigDecimal.valueOf(armorPenetration),
                BigDecimal.valueOf(resistance),
                movement,
                BigDecimal.valueOf(control),
                ownerId);
    }

    private BigDecimal setPrecision(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal setPrecision(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 执行带反击的攻击
     */
    public RetaliationCombatCalculator.RetaliationResult attackWithRetaliation(CombatUnit target) {
        RetaliationCombatCalculator.RetaliationResult result =
                RetaliationCombatCalculator.calculateCombat(this, target);

        if (result.isInRange()) {
            // 应用攻击者的伤害
            target.applyDamageResult(result.getAttackerDamage());

            // 如果有反击，应用反击伤害
            if (result.hasRetaliation()) {
                this.applyDamageResult(result.getRetaliationDamage());
            }
        }

        return result;
    }

    /**
     * 接收伤害攻击
     */
    public DamageResult takeDamage(CombatUnit attacker) {
        CombatResult combatResult = DefenseFirstCombatCalculator.calculateCombat(
                attacker.getAttack(),
                this.currentDefense,
                attacker.getArmorPenetration(),
                this.resistance,
                attacker.getDamageMultiplier(),
                attacker.getExtraPenetration(),
                this.extraDefense
        );

        return applyDamageResult(combatResult);
    }

    /**
     * 应用战斗结果到单位
     */
    private DamageResult applyDamageResult(CombatResult combatResult) {
        BigDecimal oldDefense = this.currentDefense;
        BigDecimal oldHealth = this.currentHealth;

        this.currentDefense = this.currentDefense.subtract(combatResult.getDefenseDamage())
                .max(BigDecimal.ZERO);
        BigDecimal defenseDamage = oldDefense.subtract(this.currentDefense);

        this.currentHealth = this.currentHealth.subtract(combatResult.getHealthDamage())
                .max(BigDecimal.ZERO);
        BigDecimal healthDamage = oldHealth.subtract(this.currentHealth);

        boolean killed = !this.isAlive();
        boolean defenseBreached = this.currentDefense.compareTo(BigDecimal.ZERO) == 0;

        return new DamageResult(combatResult, defenseDamage, healthDamage, killed, defenseBreached);
    }

    /**
     * 修复防御力
     */
    public void repairDefense(BigDecimal repairAmount) {
        this.currentDefense = this.currentDefense.add(repairAmount)
                .min(this.maxDefense)
                .max(BigDecimal.ZERO);
    }

    /**
     * 完全修复防御力
     */
    public void fullyRepairDefense() {
        this.currentDefense = this.maxDefense;
    }

    /**
     * 获取防御力百分比
     */
    public BigDecimal getDefensePercentage() {
        if (this.maxDefense.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return this.currentDefense.divide(this.maxDefense, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 检查防御是否完整
     */
    public boolean isDefenseIntact() {
        return this.currentDefense.compareTo(this.maxDefense) == 0;
    }

    /**
     * 检查防御是否被击穿
     */
    public boolean isDefenseBreached() {
        return this.currentDefense.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal takeDamage(BigDecimal damage, BigDecimal penetration) {
        BigDecimal effectiveDefense = this.defense.multiply(
                BigDecimal.ONE.subtract(penetration.min(BigDecimal.ONE))
        );
        BigDecimal baseDamage = damage.subtract(effectiveDefense).max(BigDecimal.ZERO);
        BigDecimal finalDamage = baseDamage.multiply(
                BigDecimal.ONE.subtract(this.resistance.min(BigDecimal.ONE))
        );
        finalDamage = setPrecision(finalDamage);
        this.currentHealth = this.currentHealth.subtract(finalDamage).max(BigDecimal.ZERO);
        return finalDamage;
    }

    public BigDecimal receiveHeal(BigDecimal healAmount) {
        BigDecimal oldHealth = this.currentHealth;
        this.currentHealth = this.currentHealth.add(healAmount).min(this.maxHealth);
        BigDecimal actualHeal = this.currentHealth.subtract(oldHealth);
        return setPrecision(actualHeal);
    }

    public boolean isAlive() {
        return this.currentHealth.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getHealthPercentage() {
        if (this.maxHealth.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return setPrecision(
                this.currentHealth.divide(this.maxHealth, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
        );
    }

    public void resetTurnState() {
        this.hasMovedThisTurn = false;
        this.hasActedThisTurn = false;
    }

    public boolean canMove() {
        return isAlive() && isDeployed && !hasMovedThisTurn && movement > 0;
    }

    public boolean canAct() {
        return isAlive() && isDeployed && !hasActedThisTurn;
    }

    // Getter和Setter方法
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAttack() { return attack; }
    public void setAttack(BigDecimal attack) { this.attack = setPrecision(attack); }
    public void setAttack(double attack) { this.attack = setPrecision(attack); }
    public BigDecimal getDefense() { return defense; }
    public void setDefense(BigDecimal defense) { this.defense = setPrecision(defense); }
    public void setDefense(double defense) { this.defense = setPrecision(defense); }
    public BigDecimal getMaxHealth() { return maxHealth; }
    public void setMaxHealth(BigDecimal maxHealth) {
        this.maxHealth = setPrecision(maxHealth);
        this.currentHealth = this.currentHealth.min(this.maxHealth);
    }
    public BigDecimal getCurrentHealth() { return currentHealth; }
    public BigDecimal getArmorPenetration() { return armorPenetration; }
    public void setArmorPenetration(BigDecimal armorPenetration) {
        this.armorPenetration = setPrecision(armorPenetration);
    }
    public BigDecimal getResistance() { return resistance; }
    public void setResistance(BigDecimal resistance) {
        this.resistance = setPrecision(resistance);
    }
    public int getMovement() { return movement; }
    public void setMovement(int movement) { this.movement = movement; }
    public BigDecimal getControl() { return control; }
    public void setControl(BigDecimal control) { this.control = setPrecision(control); }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public boolean isDeployed() { return isDeployed; }
    public void setDeployed(boolean deployed) { isDeployed = deployed; }
    public CombatStatus getStatus() { return status; }
    public void setStatus(CombatStatus status) { this.status = status; }
    public boolean hasMovedThisTurn() { return hasMovedThisTurn; }
    public void setHasMovedThisTurn(boolean hasMovedThisTurn) { this.hasMovedThisTurn = hasMovedThisTurn; }
    public boolean hasActedThisTurn() { return hasActedThisTurn; }
    public void setHasActedThisTurn(boolean hasActedThisTurn) { this.hasActedThisTurn = hasActedThisTurn; }
    public CombatRange getCombatRange() { return combatRange; }
    public void setCombatRange(CombatRange combatRange) { this.combatRange = combatRange; }

    // 新增Getter和Setter方法
    public BigDecimal getCurrentDefense() { return currentDefense; }
    public BigDecimal getMaxDefense() { return maxDefense; }
    public BigDecimal getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(BigDecimal damageMultiplier) {
        this.damageMultiplier = damageMultiplier.setScale(2, RoundingMode.HALF_UP);
    }
    public BigDecimal getExtraPenetration() { return extraPenetration; }
    public void setExtraPenetration(BigDecimal extraPenetration) {
        this.extraPenetration = extraPenetration.setScale(2, RoundingMode.HALF_UP);
    }
    public BigDecimal getExtraDefense() { return extraDefense; }
    public void setExtraDefense(BigDecimal extraDefense) {
        this.extraDefense = extraDefense.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return String.format("%s [%s] 生命: %s/%s 防御: %s/%s 攻击: %s",
                name, type, currentHealth, maxHealth, currentDefense, maxDefense, attack);
    }

    /**
     * 获取详细状态信息
     */
    public String getDetailedStatus() {
        return String.format("%s - 生命: %.1f/%.1f (%.1f%%) 防御: %.1f/%.1f (%.1f%%)",
                name, currentHealth, maxHealth, getHealthPercentage(),
                currentDefense, maxDefense, getDefensePercentage());
    }

    /**
     * 伤害结果详细类
     */
    public static class DamageResult {
        private final CombatResult combatResult;
        private final BigDecimal defenseDamage;
        private final BigDecimal healthDamage;
        private final boolean targetKilled;
        private final boolean defenseBreached;

        public DamageResult(CombatResult combatResult, BigDecimal defenseDamage,
                            BigDecimal healthDamage, boolean targetKilled, boolean defenseBreached) {
            this.combatResult = combatResult;
            this.defenseDamage = defenseDamage;
            this.healthDamage = healthDamage;
            this.targetKilled = targetKilled;
            this.defenseBreached = defenseBreached;
        }

        public CombatResult getCombatResult() { return combatResult; }
        public BigDecimal getDefenseDamage() { return defenseDamage; }
        public BigDecimal getHealthDamage() { return healthDamage; }
        public boolean isTargetKilled() { return targetKilled; }
        public boolean isDefenseBreached() { return defenseBreached; }

        public String getDamageReport() {
            StringBuilder report = new StringBuilder();
            report.append(combatResult.getCombatReport());
            report.append(String.format("防御损伤: %.2f\n", defenseDamage));
            report.append(String.format("生命损伤: %.2f\n", healthDamage));
            if (defenseBreached) {
                report.append("⚠️ 防御被击穿!\n");
            }
            if (targetKilled) {
                report.append("💀 目标被消灭!\n");
            }
            return report.toString();
        }
    }

    /**
     * 攻击结果详细类
     */
    public static class AttackResult {
        private final CombatUnit attacker;
        private final CombatUnit target;
        private final DamageResult damageResult;

        public AttackResult(CombatUnit attacker, CombatUnit target, DamageResult damageResult) {
            this.attacker = attacker;
            this.target = target;
            this.damageResult = damageResult;
        }

        public CombatUnit getAttacker() { return attacker; }
        public CombatUnit getTarget() { return target; }
        public DamageResult getDamageResult() { return damageResult; }

        public String getAttackReport() {
            StringBuilder report = new StringBuilder();
            report.append(String.format("%s 攻击 %s\n", attacker.getName(), target.getName()));
            report.append(damageResult.getDamageReport());
            return report.toString();
        }
    }
}