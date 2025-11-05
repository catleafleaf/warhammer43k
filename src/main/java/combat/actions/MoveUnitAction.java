package combat.actions;

import combat.model.CombatUnit;
import combat.model.Position;

/**
 * 移动单位动作
 */
public class MoveUnitAction extends CombatAction {
    private final Position targetPosition;

    public MoveUnitAction(CombatUnit sourceUnit, Position targetPosition) {
        super("移动单位", sourceUnit, ActionType.MOVE);
        this.targetPosition = targetPosition;
    }

    @Override
    public ValidationResult validate() {
        if (!sourceUnit.isDeployed()) {
            return ValidationResult.failure("单位未部署，无法移动");
        }
        if (!sourceUnit.isAlive()) {
            return ValidationResult.failure("单位已阵亡，无法移动");
        }
        if (sourceUnit.hasMovedThisTurn()) {
            return ValidationResult.failure("单位本回合已移动");
        }
        int distance = sourceUnit.getPosition().distanceTo(targetPosition);
        if (distance > sourceUnit.getMovement()) {
            return ValidationResult.failure("移动距离超出单位移动力");
        }
        if (targetPosition.getX() < 0 || targetPosition.getY() < 0) {
            return ValidationResult.failure("无效的目标位置");
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
            Position oldPosition = sourceUnit.getPosition();
            sourceUnit.setPosition(targetPosition);
            sourceUnit.setHasMovedThisTurn(true);
            this.executed = true;

            return ActionResult.success(
                    String.format("%s 从 %s 移动到 %s",
                            sourceUnit.getName(), oldPosition, targetPosition)
            );

        } catch (Exception e) {
            return ActionResult.failure("移动失败: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return String.format("将 %s 移动到位置 %s", sourceUnit.getName(), targetPosition);
    }

    public Position getTargetPosition() {
        return targetPosition;
    }
}