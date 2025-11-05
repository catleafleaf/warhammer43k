package combat.actions;

import combat.model.CombatUnit;
import combat.model.CombatResource;
import combat.model.Position;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

/**
 * 使用物资动作
 */
public class UseResourceAction extends CombatAction {
    private final CombatResource resource;
    private final List<CombatUnit> targetUnits;
    private final Position targetPosition;

    public UseResourceAction(CombatUnit sourceUnit, CombatResource resource, List<CombatUnit> targetUnits) {
        super("使用物资", sourceUnit, ActionType.USE_RESOURCE);
        this.resource = resource;
        this.targetUnits = new ArrayList<>(targetUnits);
        this.targetPosition = null;
    }

    public UseResourceAction(CombatUnit sourceUnit, CombatResource resource) {
        super("使用物资", sourceUnit, ActionType.USE_RESOURCE);
        this.resource = resource;
        this.targetUnits = new ArrayList<>();
        this.targetPosition = null;
    }

    public UseResourceAction(CombatUnit sourceUnit, CombatResource resource, Position targetPosition) {
        super("使用物资", sourceUnit, ActionType.USE_RESOURCE);
        this.resource = resource;
        this.targetUnits = new ArrayList<>();
        this.targetPosition = targetPosition;
    }

    @Override
    public ValidationResult validate() {
        if (!sourceUnit.isDeployed()) {
            return ValidationResult.failure("单位未部署，无法使用物资");
        }
        if (!sourceUnit.isAlive()) {
            return ValidationResult.failure("单位已阵亡，无法使用物资");
        }
        if (sourceUnit.hasActedThisTurn()) {
            return ValidationResult.failure("单位本回合已行动");
        }
        if (!resource.isAvailable()) {
            return ValidationResult.failure("物资不可用（数量不足或冷却中）");
        }

        switch (resource.getTargetingType()) {
            case NONE:
                if (!targetUnits.isEmpty() || targetPosition != null) {
                    return ValidationResult.failure("非指向性物资不需要指定目标");
                }
                break;
            case SINGLE:
                if (targetUnits.size() != 1) {
                    return ValidationResult.failure("单体目标物资需要指定一个目标");
                }
                break;
            case MULTIPLE:
                if (targetUnits.isEmpty()) {
                    return ValidationResult.failure("多目标物资需要指定至少一个目标");
                }
                if (targetUnits.size() > resource.getMaxTargets()) {
                    return ValidationResult.failure("目标数量超过物资最大目标数");
                }
                break;
            case AREA:
                if (targetPosition == null) {
                    return ValidationResult.failure("区域目标物资需要指定目标位置");
                }
                break;
            case SELF:
                if (targetUnits.size() != 1 || !targetUnits.get(0).equals(sourceUnit)) {
                    return ValidationResult.failure("该物资只能对自身使用");
                }
                break;
        }

        for (CombatUnit target : targetUnits) {
            if (!resource.isValidTarget(target, sourceUnit)) {
                return ValidationResult.failure("无效的目标: " + target.getName());
            }
        }

        return ValidationResult.success();
    }

    @Override
    public ActionResult execute() {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return ActionResult.failure(validation.getMessage());
        }

        try {
            if (!resource.use()) {
                return ActionResult.failure("物资使用失败");
            }

            List<String> effectResults = new ArrayList<>();
            for (CombatUnit target : targetUnits) {
                String result = applyResourceEffect(target);
                effectResults.add(result);
            }

            sourceUnit.setHasActedThisTurn(true);
            this.executed = true;

            return ActionResult.success(
                    String.format("%s 使用了 %s", sourceUnit.getName(), resource.getName()),
                    effectResults
            );

        } catch (Exception e) {
            return ActionResult.failure("物资使用失败: " + e.getMessage());
        }
    }

    private String applyResourceEffect(CombatUnit target) {
        switch (resource.getType()) {
            case DAMAGE:
                BigDecimal damage = target.takeDamage(
                        resource.getEffectValue(),
                        resource.getPenetrationBonus()
                );
                return String.format("%s 受到 %s 点伤害", target.getName(), damage);
            case HEAL:
                BigDecimal heal = target.receiveHeal(resource.getEffectValue());
                return String.format("%s 恢复 %s 点生命值", target.getName(), heal);
            case CONTROL:
                target.setStatus(combat.model.CombatStatus.STUNNED);
                return String.format("%s 被眩晕", target.getName());
            default:
                return String.format("%s 受到 %s 效果", target.getName(), resource.getType().getDescription());
        }
    }

    @Override
    public String getDescription() {
        return String.format("%s 使用 %s", sourceUnit.getName(), resource.getName());
    }

    public CombatResource getResource() {
        return resource;
    }

    public List<CombatUnit> getTargetUnits() {
        return new ArrayList<>(targetUnits);
    }

    public Position getTargetPosition() {
        return targetPosition;
    }
}