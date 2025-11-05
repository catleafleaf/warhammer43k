package combat.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 战斗物资类
 */
public class CombatResource {
    private final String id;
    private String name;
    private String description;
    private ResourceType type;
    private TargetingType targetingType;
    private int quantity;
    private BigDecimal effectValue;
    private BigDecimal penetrationBonus;
    private BigDecimal controlEffect;
    private int areaOfEffect;
    private int maxTargets;
    private int cooldown;
    private int currentCooldown;
    private boolean consumable;

    public CombatResource(String name, ResourceType type, TargetingType targetingType,
                          int quantity, BigDecimal effectValue) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.targetingType = targetingType;
        this.quantity = quantity;
        this.effectValue = effectValue.setScale(2, RoundingMode.HALF_UP);
        this.penetrationBonus = BigDecimal.ZERO;
        this.controlEffect = BigDecimal.ZERO;
        this.areaOfEffect = 0;
        this.maxTargets = 1;
        this.cooldown = 0;
        this.currentCooldown = 0;
        this.consumable = true;
    }

    public boolean isAvailable() {
        return quantity > 0 && currentCooldown == 0;
    }

    public boolean use() {
        if (!isAvailable()) {
            return false;
        }
        if (consumable) {
            quantity--;
        }
        if (cooldown > 0) {
            currentCooldown = cooldown;
        }
        return true;
    }

    public void updateCooldown() {
        if (currentCooldown > 0) {
            currentCooldown--;
        }
    }

    public boolean isValidTarget(CombatUnit target, CombatUnit user) {
        switch (type) {
            case DAMAGE:
            case CONTROL:
                return !target.getOwnerId().equals(user.getOwnerId());
            case HEAL:
            case BUFF:
                return target.getOwnerId().equals(user.getOwnerId());
            case AOE_DAMAGE:
            case AOE_HEAL:
                return true;
            default:
                return false;
        }
    }

    // Getter和Setter
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ResourceType getType() { return type; }
    public void setType(ResourceType type) { this.type = type; }
    public TargetingType getTargetingType() { return targetingType; }
    public void setTargetingType(TargetingType targetingType) { this.targetingType = targetingType; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getEffectValue() { return effectValue; }
    public void setEffectValue(BigDecimal effectValue) {
        this.effectValue = effectValue.setScale(2, RoundingMode.HALF_UP);
    }
    public BigDecimal getPenetrationBonus() { return penetrationBonus; }
    public void setPenetrationBonus(BigDecimal penetrationBonus) {
        this.penetrationBonus = penetrationBonus.setScale(2, RoundingMode.HALF_UP);
    }
    public BigDecimal getControlEffect() { return controlEffect; }
    public void setControlEffect(BigDecimal controlEffect) {
        this.controlEffect = controlEffect.setScale(2, RoundingMode.HALF_UP);
    }
    public int getAreaOfEffect() { return areaOfEffect; }
    public void setAreaOfEffect(int areaOfEffect) { this.areaOfEffect = areaOfEffect; }
    public int getMaxTargets() { return maxTargets; }
    public void setMaxTargets(int maxTargets) { this.maxTargets = maxTargets; }
    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
    public int getCurrentCooldown() { return currentCooldown; }
    public void setCurrentCooldown(int currentCooldown) { this.currentCooldown = currentCooldown; }
    public boolean isConsumable() { return consumable; }
    public void setConsumable(boolean consumable) { this.consumable = consumable; }
}