package combat.actions;

import combat.model.CombatUnit;
import combat.model.Position;

/**
 * 部署单位动作
 */
public class DeployUnitAction extends CombatAction {
    private final Position targetPosition;

    public DeployUnitAction(CombatUnit sourceUnit, Position targetPosition) {
        super("部署单位", sourceUnit, ActionType.DEPLOY);
        this.targetPosition = targetPosition;
    }

    @Override
    public ValidationResult validate() {
        if (sourceUnit.isDeployed()) {
            return ValidationResult.failure("单位已部署");
        }
        if (targetPosition.getX() < 0 || targetPosition.getY() < 0) {
            return ValidationResult.failure("无效的部署位置");
        }
        if (!sourceUnit.isAlive()) {
            return ValidationResult.failure("单位无法部署");
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
            sourceUnit.setPosition(targetPosition);
            sourceUnit.setDeployed(true);
            sourceUnit.setHasMovedThisTurn(true);
            sourceUnit.setHasActedThisTurn(true);
            this.executed = true;

            return ActionResult.success(
                    String.format("%s 已部署到位置 %s", sourceUnit.getName(), targetPosition)
            );

        } catch (Exception e) {
            return ActionResult.failure("部署失败: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return String.format("将 %s 部署到位置 %s", sourceUnit.getName(), targetPosition);
    }

    public Position getTargetPosition() {
        return targetPosition;
    }
}