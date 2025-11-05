package combat.actions;

import combat.model.CombatUnit;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

/**
 * 攻击动作
 */
public class AttackAction extends combat.actions.CombatAction {
    private final List<CombatUnit> targets;
    private final boolean isMultiTarget;

    public AttackAction(CombatUnit sourceUnit, CombatUnit singleTarget) {
        super("攻击", sourceUnit, ActionType.ATTACK);
        this.targets = new ArrayList<>();
        this.targets.add(singleTarget);
        this.isMultiTarget = false;
    }

    public AttackAction(CombatUnit sourceUnit, List<CombatUnit> multipleTargets) {
        super("多重攻击", sourceUnit, ActionType.ATTACK);
        this.targets = new ArrayList<>(multipleTargets);
        this.isMultiTarget = true;
    }

    @Override
    public ValidationResult validate() {
        if (!sourceUnit.isDeployed()) {
            return ValidationResult.failure("单位未部署，无法攻击");
        }
        if (!sourceUnit.isAlive()) {
            return ValidationResult.failure("单位已阵亡，无法攻击");
        }
        if (sourceUnit.hasActedThisTurn()) {
            return ValidationResult.failure("单位本回合已行动");
        }
        if (targets.isEmpty()) {
            return ValidationResult.failure("没有指定攻击目标");
        }
        for (CombatUnit target : targets) {
            if (!target.isDeployed()) {
                return ValidationResult.failure("目标单位未部署");
            }
            if (!target.isAlive()) {
                return ValidationResult.failure("目标单位已阵亡");
            }
            if (target.getOwnerId().equals(sourceUnit.getOwnerId())) {
                return ValidationResult.failure("不能攻击友军单位");
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
            List<String> attackResults = new ArrayList<>();

            for (CombatUnit target : targets) {
                BigDecimal rawDamage = sourceUnit.getAttack();
                BigDecimal penetration = sourceUnit.getArmorPenetration();
                BigDecimal actualDamage = target.takeDamage(rawDamage, penetration);

                attackResults.add(String.format("%s 对 %s 造成 %s 点伤害",
                        sourceUnit.getName(), target.getName(), actualDamage));

                if (!target.isAlive()) {
                    attackResults.add(String.format("%s 被消灭!", target.getName()));
                }
            }

            sourceUnit.setHasActedThisTurn(true);
            this.executed = true;

            return ActionResult.success(
                    String.join("; ", attackResults),
                    attackResults
            );

        } catch (Exception e) {
            return ActionResult.failure("攻击执行失败: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        if (isMultiTarget) {
            return String.format("%s 对 %d 个目标进行攻击",
                    sourceUnit.getName(), targets.size());
        } else {
            return String.format("%s 攻击 %s",
                    sourceUnit.getName(), targets.get(0).getName());
        }
    }

    public List<CombatUnit> getTargets() {
        return new ArrayList<>(targets);
    }

    public boolean isMultiTarget() {
        return isMultiTarget;
    }
}