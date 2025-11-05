
import model.*;
import model.Character;
import service.CharacterService;
import service.CampaignService;
import service.DatabaseUserService;
import config.DatabaseConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 战锤40k角色与战役管理系统主程序
 * 数据库持久化版本
 */
public class Main {
    private static final CharacterService characterService = CharacterService.getInstance();
    private static final CampaignService campaignService = CampaignService.getInstance();
    private static final DatabaseUserService userService = DatabaseUserService.getInstance();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // 测试数据库连接
        System.out.println("正在测试数据库连接...");
        if (DatabaseConfig.testConnection()) {
            System.out.println("✅ 数据库连接成功！");
        } else {
            System.out.println("❌ 数据库连接失败！");
            System.out.println("请检查：");
            System.out.println("1. MySQL95 服务是否运行");
            System.out.println("2. 数据库配置是否正确");
            System.out.println("3. 数据库用户名密码是否正确");
            return;
        }

        System.out.println("=========================================");
        System.out.println("    战锤40k角色与战役管理系统");
        System.out.println("          数据库持久化版本");
        System.out.println("=========================================");

        // 首先要求用户登录
        if (!loginMenu()) {
            System.out.println("登录失败，系统退出");
            DatabaseConfig.closeDataSource();
            return;
        }

        // 检查用户是否有关联角色，如果没有则先创建角色
        if (!checkAndCreateCharacter()) {
            System.out.println("角色创建失败，系统退出");
            DatabaseConfig.closeDataSource();
            return;
        }

        // 进入主系统
        mainSystem();
    }

    // 登录菜单
    private static boolean loginMenu() {
        while (true) {
            System.out.println("\n--- 用户登录 ---");
            System.out.println("1. 登录");
            System.out.println("2. 注册");
            System.out.println("3. 退出系统");
            System.out.print("请选择操作: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    return login();
                case "2":
                    return register();
                case "3":
                    return false;
                default:
                    System.out.println("无效选择，请重新输入。");
            }
        }
    }

    // 用户登录
    private static boolean login() {
        System.out.println("\n--- 用户登录 ---");

        System.out.print("用户名: ");
        String username = scanner.nextLine().trim();

        System.out.print("密码: ");
        String password = scanner.nextLine().trim();

        try {
            User user = userService.login(username, password);
            userService.setCurrentUser(user);
            System.out.println("登录成功！欢迎 " + user.getUsername() +
                    " (" + (user.isManager() ? "管理者" : "玩家") + ")");
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println("登录失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("登录失败: 系统错误 - " + e.getMessage());
            return false;
        }
    }

    // 用户注册 - 修改：移除权限选择，默认注册为玩家
    private static boolean register() {
        System.out.println("\n--- 用户注册 ---");

        System.out.print("请输入用户名 (3-20位字母、数字、下划线): ");
        String username = scanner.nextLine().trim();

        if (!userService.isUsernameAvailable(username)) {
            System.out.println("用户名已存在，请选择其他用户名");
            return false;
        }

        System.out.print("请输入密码 (至少6位): ");
        String password = scanner.nextLine().trim();

        System.out.print("确认密码: ");
        String confirmPassword = scanner.nextLine().trim();

        if (!password.equals(confirmPassword)) {
            System.out.println("两次输入的密码不一致");
            return false;
        }

        // 移除权限选择，默认注册为玩家（权限等级0）
        int permissionLevel = 0;

        try {
            // 注册用户，characterId设为0表示需要创建角色
            User user = userService.registerUser(username, password, permissionLevel, 0);
            userService.setCurrentUser(user);

            System.out.println("注册成功！欢迎 " + user.getUsername());
            System.out.println("您已注册为玩家，如需管理员权限请联系现有管理员。");
            return true;

        } catch (IllegalArgumentException e) {
            System.out.println("注册失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("注册失败: 系统错误 - " + e.getMessage());
            return false;
        }
    }

    // 检查并创建角色
    private static boolean checkAndCreateCharacter() {
        User currentUser = userService.getCurrentUser();

        // 如果用户已经有关联角色，直接返回成功
        if (userService.hasLinkedCharacter(currentUser.getId())) {
            Character character = characterService.findCharacterById(currentUser.getCharacterId());
            if (character != null) {
                System.out.println("欢迎回来，" + character.getName() + "!");
                return true;
            } else {
                System.out.println("关联的角色不存在，需要重新创建角色");
            }
        }

        // 创建角色流程
        System.out.println("\n=== 角色创建 ===");
        System.out.println("欢迎新用户！在进入系统前，请先创建您的角色。");

        Character character = createCharacterInteractive();
        if (character != null) {
            // 关联角色到用户
            boolean linked = userService.linkCharacterToUser(currentUser.getId(), character.getId());
            if (linked) {
                System.out.println("角色创建成功！欢迎，" + character.getName() + "!");
                return true;
            } else {
                System.out.println("角色关联失败");
                return false;
            }
        } else {
            System.out.println("角色创建失败");
            return false;
        }
    }

    // 交互式角色创建
    private static Character createCharacterInteractive() {
        System.out.println("\n--- 创建您的角色 ---");

        System.out.print("输入角色名称: ");
        String name = scanner.nextLine().trim();

        System.out.println("选择阵营:");
        Faction[] factions = Faction.values();
        for (int i = 0; i < factions.length; i++) {
            System.out.printf("%d. %s - %s\n", i + 1,
                    factions[i].getDisplayName(), factions[i].getBattleCry());
        }
        System.out.print("请选择阵营编号: ");

        try {
            int factionIndex = Integer.parseInt(scanner.nextLine()) - 1;
            if (factionIndex < 0 || factionIndex >= factions.length) {
                System.out.println("无效的阵营编号！");
                return null;
            }

            Faction faction = factions[factionIndex];
            Character character = characterService.createCharacter(name, faction);

            // 可选：添加初始单位和资源
            System.out.print("是否为角色添加初始单位？(y/N): ");
            String addUnitChoice = scanner.nextLine().trim();
            if ("y".equalsIgnoreCase(addUnitChoice)) {
                addInitialUnits(character);
            }

            System.out.print("是否为角色添加初始资源？(y/N): ");
            String addResourceChoice = scanner.nextLine().trim();
            if ("y".equalsIgnoreCase(addResourceChoice)) {
                addInitialResources(character);
            }

            return character;

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
            return null;
        } catch (IllegalArgumentException e) {
            System.out.println("创建角色失败: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("创建角色失败: 系统错误 - " + e.getMessage());
            return null;
        }
    }

    // 添加初始单位
    private static void addInitialUnits(Character character) {
        System.out.println("\n--- 添加初始单位 ---");

        while (true) {
            System.out.print("输入单位名称 (输入'done'结束): ");
            String unitName = scanner.nextLine().trim();

            if ("done".equalsIgnoreCase(unitName)) {
                break;
            }

            System.out.print("输入单位类型: ");
            String unitType = scanner.nextLine().trim();

            System.out.print("输入单位数量: ");
            try {
                int count = Integer.parseInt(scanner.nextLine());

                Unit unit = new Unit(unitName, unitType, count);
                characterService.addUnitToCharacter(character.getId(), unit);
                System.out.println("单位添加成功！");

            } catch (NumberFormatException e) {
                System.out.println("请输入有效的数字！");
            } catch (IllegalArgumentException e) {
                System.out.println("添加单位失败: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("添加单位失败: 系统错误 - " + e.getMessage());
            }
        }
    }

    // 添加初始资源
    private static void addInitialResources(Character character) {
        System.out.println("\n--- 添加初始资源 ---");

        while (true) {
            System.out.print("输入资源名称 (输入'done'结束): ");
            String resourceName = scanner.nextLine().trim();

            if ("done".equalsIgnoreCase(resourceName)) {
                break;
            }

            System.out.print("输入资源数量: ");
            try {
                int quantity = Integer.parseInt(scanner.nextLine());

                Resource resource = new Resource(resourceName, quantity);
                characterService.addResourceToCharacter(character.getId(), resource);
                System.out.println("资源添加成功！");

            } catch (NumberFormatException e) {
                System.out.println("请输入有效的数字！");
            } catch (IllegalArgumentException e) {
                System.out.println("添加资源失败: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("添加资源失败: 系统错误 - " + e.getMessage());
            }
        }
    }

    // 主系统
    private static void mainSystem() {
        while (true) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    createCharacter();
                    break;
                case "2":
                    listCharacters();
                    break;
                case "3":
                    viewCharacter();
                    break;
                case "4":
                    updateCharacter();
                    break;
                case "5":
                    deleteCharacter();
                    break;
                case "6":
                    addUnitToCharacter();
                    break;
                case "7":
                    addResourceToCharacter();
                    break;
                case "8":
                    campaignMenu();
                    break;
                case "9":
                    userManagementMenu();
                    break;
                case "10":
                    if (switchUser()) {
                        // 切换用户后需要检查角色
                        if (!checkAndCreateCharacter()) {
                            System.out.println("角色创建失败，返回主菜单");
                        }
                    }
                    break;
                case "11":
                    System.out.println("感谢使用，再见！");
                    DatabaseConfig.closeDataSource();
                    return;
                default:
                    System.out.println("无效选择，请重新输入。");
            }

            System.out.println();
        }
    }

    private static void printMainMenu() {
        User currentUser = userService.getCurrentUser();
        Character userCharacter = null;
        if (currentUser != null && currentUser.getCharacterId() != 0) {
            userCharacter = characterService.findCharacterById(currentUser.getCharacterId());
        }

        System.out.println("\n主菜单 - 当前用户: " + (currentUser != null ? currentUser.getUsername() : "未知") +
                " (" + (currentUser != null && currentUser.isManager() ? "管理者" : "玩家") + ")");
        if (userCharacter != null) {
            System.out.println("当前角色: " + userCharacter.getName() +
                    " [" + userCharacter.getFaction().getDisplayName() + "]");
        }
        System.out.println("1. 创建角色");
        System.out.println("2. 列出所有角色");
        System.out.println("3. 查看角色详情");
        System.out.println("4. 更新角色信息");
        System.out.println("5. 删除角色");
        System.out.println("6. 为角色添加单位");
        System.out.println("7. 为角色添加资源");
        System.out.println("8. 战役系统");
        System.out.println("9. 用户管理");
        System.out.println("10. 切换用户");
        System.out.println("11. 退出");
        System.out.print("请选择操作: ");
    }

    // ==================== 角色管理相关方法 ====================

    private static void createCharacter() {
        System.out.println("\n--- 创建新角色 ---");

        System.out.print("输入角色名称: ");
        String name = scanner.nextLine().trim();

        System.out.println("选择阵营:");
        for (Faction faction : Faction.values()) {
            System.out.printf("%d. %s - %s\n", faction.ordinal() + 1,
                    faction.getDisplayName(), faction.getBattleCry());
        }
        System.out.print("请选择阵营编号: ");

        try {
            int factionIndex = Integer.parseInt(scanner.nextLine()) - 1;
            if (factionIndex < 0 || factionIndex >= Faction.values().length) {
                System.out.println("无效的阵营编号！");
                return;
            }

            Faction faction = Faction.values()[factionIndex];
            Character character = characterService.createCharacter(name, faction);
            System.out.println("角色创建成功！");
            System.out.println("角色ID: " + character.getId());

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
        } catch (IllegalArgumentException e) {
            System.out.println("创建角色失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("创建角色失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void listCharacters() {
        System.out.println("\n--- 所有角色 ---");
        List<Character> characters = characterService.getAllCharacters();

        if (characters.isEmpty()) {
            System.out.println("暂无角色数据。");
            return;
        }

        System.out.printf("%-10s %-20s %-15s %-10s %-10s\n",
                "ID", "名称", "称号", "阵营", "单位数量");
        System.out.println("----------------------------------------------------------------");

        for (Character character : characters) {
            System.out.printf("%-10d %-20s %-15s %-10s %-10d\n",
                    character.getId(),
                    character.getName(),
                    character.getTitle().isEmpty() ? "无" : character.getTitle(),
                    character.getFaction().getDisplayName(),
                    character.getUnits().size());
        }
    }

    private static void viewCharacter() {
        System.out.print("输入要查看的角色ID: ");
        try {
            long id = Long.parseLong(scanner.nextLine());
            Character character = characterService.findCharacterById(id);

            if (character == null) {
                System.out.println("未找到该角色！");
                return;
            }

            printCharacterDetail(character);

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的角色ID！");
        } catch (Exception e) {
            System.out.println("查看角色失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void updateCharacter() {
        System.out.print("输入要更新的角色ID: ");
        try {
            long id = Long.parseLong(scanner.nextLine());
            Character character = characterService.findCharacterById(id);

            if (character == null) {
                System.out.println("未找到该角色！");
                return;
            }

            System.out.println("当前角色信息:");
            printCharacterDetail(character);

            System.out.print("输入新名称 (直接回车保持原值): ");
            String newName = scanner.nextLine().trim();
            if (!newName.isEmpty()) {
                character.setName(newName);
            }

            System.out.print("输入新称号 (直接回车保持原值): ");
            String newTitle = scanner.nextLine().trim();
            if (!newTitle.isEmpty()) {
                character.setTitle(newTitle);
            }

            characterService.updateCharacter(character);
            System.out.println("角色更新成功！");

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的角色ID！");
        } catch (IllegalArgumentException e) {
            System.out.println("更新角色失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("更新角色失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void deleteCharacter() {
        System.out.print("输入要删除的角色ID: ");
        try {
            long id = Long.parseLong(scanner.nextLine());
            Character character = characterService.findCharacterById(id);

            if (character == null) {
                System.out.println("未找到该角色！");
                return;
            }

            // 检查是否是当前用户关联的角色
            User currentUser = userService.getCurrentUser();
            if (currentUser != null && currentUser.getCharacterId() == id) {
                System.out.println("不能删除当前关联的角色！");
                return;
            }

            System.out.println("即将删除角色: " + character.getName());
            System.out.print("确认删除？(y/N): ");
            String confirmation = scanner.nextLine().trim();

            if ("y".equalsIgnoreCase(confirmation)) {
                boolean success = characterService.deleteCharacter(id);
                if (success) {
                    System.out.println("角色删除成功！");
                } else {
                    System.out.println("角色删除失败！");
                }
            } else {
                System.out.println("取消删除操作。");
            }

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的角色ID！");
        } catch (Exception e) {
            System.out.println("删除角色失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void addUnitToCharacter() {
        System.out.print("输入角色ID: ");
        try {
            long id = Long.parseLong(scanner.nextLine());
            Character character = characterService.findCharacterById(id);

            if (character == null) {
                System.out.println("未找到该角色！");
                return;
            }

            System.out.print("输入单位名称: ");
            String unitName = scanner.nextLine().trim();

            System.out.print("输入单位类型: ");
            String unitType = scanner.nextLine().trim();

            System.out.print("输入单位数量: ");
            int count = Integer.parseInt(scanner.nextLine());

            Unit unit = new Unit(unitName, unitType, count);
            characterService.addUnitToCharacter(id, unit);
            System.out.println("单位添加成功！");

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
        } catch (IllegalArgumentException e) {
            System.out.println("添加单位失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("添加单位失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void addResourceToCharacter() {
        System.out.print("输入角色ID: ");
        try {
            long id = Long.parseLong(scanner.nextLine());
            Character character = characterService.findCharacterById(id);

            if (character == null) {
                System.out.println("未找到该角色！");
                return;
            }

            System.out.print("输入资源名称: ");
            String resourceName = scanner.nextLine().trim();

            System.out.print("输入资源数量: ");
            int quantity = Integer.parseInt(scanner.nextLine());

            Resource resource = new Resource(resourceName, quantity);
            characterService.addResourceToCharacter(id, resource);
            System.out.println("资源添加成功！");

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
        } catch (IllegalArgumentException e) {
            System.out.println("添加资源失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("添加资源失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void printCharacterDetail(Character character) {
        System.out.println("\n=== 角色详细信息 ===");
        System.out.println("ID: " + character.getId());
        System.out.println("名称: " + character.getName());
        System.out.println("称号: " + (character.getTitle().isEmpty() ? "无" : character.getTitle()));
        System.out.println("阵营: " + character.getFaction().getDisplayName());
        System.out.println("战吼: " + character.getFaction().getBattleCry());
        System.out.println("创建时间: " + new java.util.Date(character.getCreatedAt()));
        System.out.println("更新时间: " + new java.util.Date(character.getUpdatedAt()));

        System.out.println("\n--- 麾下单位 ---");
        if (character.getUnits().isEmpty()) {
            System.out.println("暂无单位");
        } else {
            for (Unit unit : character.getUnits()) {
                System.out.println("  • " + unit);
            }
        }

        System.out.println("\n--- 麾下物资 ---");
        if (character.getResources().isEmpty()) {
            System.out.println("暂无物资");
        } else {
            for (Resource resource : character.getResources()) {
                System.out.println("  • " + resource);
            }
        }
        System.out.println("==================\n");
    }

    // ==================== 战役系统相关方法 ====================

    private static void campaignMenu() {
        while (true) {
            User currentUser = userService.getCurrentUser();
            System.out.println("\n--- 战役系统 ---");
            if (currentUser != null) {
                System.out.println("当前用户: " + currentUser.getUsername() +
                        " (权限: " + (currentUser.isManager() ? "管理者" : "玩家") + ")");
            }
            System.out.println("1. 创建战役");
            System.out.println("2. 开始战役");
            System.out.println("3. 列出所有战役");
            System.out.println("4. 查看战役详情");
            System.out.println("5. 为战役添加敌人批次");
            System.out.println("6. 返回主菜单");
            System.out.print("请选择操作: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    createCampaign();
                    break;
                case "2":
                    startCampaign();
                    break;
                case "3":
                    listCampaigns();
                    break;
                case "4":
                    viewCampaign();
                    break;
                case "5":
                    addEnemyBatchToCampaign();
                    break;
                case "6":
                    return;
                default:
                    System.out.println("无效选择，请重新输入。");
            }
        }
    }

    private static void createCampaign() {
        System.out.println("\n--- 创建战役 ---");

        try {
            System.out.print("输入战役名称: ");
            String name = scanner.nextLine().trim();

            System.out.print("输入战役描述: ");
            String description = scanner.nextLine().trim();

            Campaign campaign = campaignService.createCampaign(name, description);
            System.out.println("战役创建成功！");
            System.out.println("战役ID: " + campaign.getId());

        } catch (IllegalArgumentException e) {
            System.out.println("创建战役失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("创建战役失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void startCampaign() {
        System.out.println("\n--- 开始战役 ---");

        try {
            // 选择战役
            System.out.print("输入要开始的战役ID: ");
            long campaignId = Long.parseLong(scanner.nextLine());
            Campaign campaign = campaignService.findCampaignById(campaignId);

            if (campaign == null) {
                System.out.println("未找到该战役！");
                return;
            }

            // 选择参与者
            List<Character> allCharacters = characterService.getAllCharacters();
            if (allCharacters.isEmpty()) {
                System.out.println("暂无可用角色！");
                return;
            }

            System.out.println("可用角色列表:");
            for (Character character : allCharacters) {
                System.out.printf("ID: %d, 名称: %s, 阵营: %s\n",
                        character.getId(), character.getName(), character.getFaction().getDisplayName());
            }

            System.out.print("输入要加入战役的角色ID（多个ID用逗号分隔）: ");
            String input = scanner.nextLine().trim();
            String[] idStrs = input.split(",");

            List<Long> participantIds = new ArrayList<>();
            for (String idStr : idStrs) {
                try {
                    long charId = Long.parseLong(idStr.trim());
                    participantIds.add(charId);
                } catch (NumberFormatException e) {
                    System.out.println("跳过无效ID: " + idStr);
                }
            }

            if (participantIds.isEmpty()) {
                System.out.println("未选择任何有效角色！");
                return;
            }

            boolean success = campaignService.startCampaign(campaignId, participantIds);
            if (success) {
                System.out.println("战役开始成功！");
                System.out.println("参与者数量: " + participantIds.size());
            }

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
        } catch (IllegalArgumentException e) {
            System.out.println("开始战役失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("开始战役失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void listCampaigns() {
        System.out.println("\n--- 所有战役 ---");
        List<Campaign> campaigns = campaignService.getAllCampaigns();

        if (campaigns.isEmpty()) {
            System.out.println("暂无战役数据。");
            return;
        }

        System.out.printf("%-10s %-20s %-15s %-10s %-10s\n",
                "ID", "名称", "状态", "创建者", "参与者");
        System.out.println("----------------------------------------------------------------");

        for (Campaign campaign : campaigns) {
            System.out.printf("%-10d %-20s %-15s %-10d %-10d\n",
                    campaign.getId(),
                    campaign.getName(),
                    campaign.getStatus().getDisplayName(),
                    campaign.getCreatorId(),
                    campaign.getParticipantIds().size());
        }
    }

    private static void viewCampaign() {
        System.out.print("输入要查看的战役ID: ");
        try {
            long id = Long.parseLong(scanner.nextLine());
            Campaign campaign = campaignService.findCampaignById(id);

            if (campaign == null) {
                System.out.println("未找到该战役！");
                return;
            }

            printCampaignDetail(campaign);

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的战役ID！");
        } catch (Exception e) {
            System.out.println("查看战役失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void addEnemyBatchToCampaign() {
        System.out.println("\n--- 为战役添加敌人批次 ---");

        try {
            System.out.print("输入战役ID: ");
            long campaignId = Long.parseLong(scanner.nextLine());

            System.out.print("输入批次名称: ");
            String batchName = scanner.nextLine().trim();

            System.out.print("输入批次描述: ");
            String batchDescription = scanner.nextLine().trim();

            System.out.print("输入刷新回合: ");
            int spawnRound = Integer.parseInt(scanner.nextLine());

            EnemyBatch batch = new EnemyBatch(batchName, batchDescription, spawnRound);

            // 添加敌人单位到批次
            while (true) {
                System.out.print("是否添加敌人单位？(y/N): ");
                String choice = scanner.nextLine().trim();
                if (!"y".equalsIgnoreCase(choice)) {
                    break;
                }

                System.out.print("输入敌人单位名称: ");
                String enemyName = scanner.nextLine().trim();

                System.out.print("输入敌人单位类型: ");
                String enemyType = scanner.nextLine().trim();

                System.out.print("输入敌人数量: ");
                int enemyCount = Integer.parseInt(scanner.nextLine());

                Unit enemy = new Unit(enemyName, enemyType, enemyCount);
                batch.addEnemy(enemy);
                System.out.println("敌人单位添加成功！");
            }

            campaignService.addEnemyBatchToCampaign(campaignId, batch);
            System.out.println("敌人批次添加成功！");

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
        } catch (IllegalArgumentException e) {
            System.out.println("添加敌人批次失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("添加敌人批次失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void printCampaignDetail(Campaign campaign) {
        System.out.println("\n=== 战役详细信息 ===");
        System.out.println("ID: " + campaign.getId());
        System.out.println("名称: " + campaign.getName());
        System.out.println("描述: " + campaign.getDescription());
        System.out.println("创建者ID: " + campaign.getCreatorId());
        System.out.println("状态: " + campaign.getStatus().getDisplayName());
        System.out.println("当前批次: " + (campaign.getCurrentBatchIndex() + 1));

        System.out.println("\n--- 参与者 (" + campaign.getParticipantIds().size() + "人) ---");
        if (campaign.getParticipantIds().isEmpty()) {
            System.out.println("暂无参与者");
        } else {
            for (Long participantId : campaign.getParticipantIds()) {
                Character character = characterService.findCharacterById(participantId);
                if (character != null) {
                    System.out.println("  • ID: " + character.getId() + ", 名称: " + character.getName());
                }
            }
        }

        System.out.println("\n--- 敌人批次 (" + campaign.getEnemyBatches().size() + "批) ---");
        if (campaign.getEnemyBatches().isEmpty()) {
            System.out.println("暂无敌人批次");
        } else {
            for (int i = 0; i < campaign.getEnemyBatches().size(); i++) {
                EnemyBatch batch = campaign.getEnemyBatches().get(i);
                System.out.println("  批次 " + (i + 1) + ": " + batch.getName() +
                        " (第" + batch.getSpawnRound() + "回合刷新)");
                for (Unit enemy : batch.getEnemies()) {
                    System.out.println("    - " + enemy);
                }
            }
        }
        System.out.println("==================\n");
    }

    // ==================== 用户管理相关方法 ====================

    private static void userManagementMenu() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || !currentUser.isManager()) {
            System.out.println("权限不足：只有管理者可以访问用户管理功能");
            return;
        }

        while (true) {
            System.out.println("\n--- 用户管理 ---");
            System.out.println("当前用户: " + currentUser.getUsername() + " (管理者)");
            System.out.println("1. 列出所有用户");
            System.out.println("2. 修改用户权限");
            System.out.println("3. 指定用户为管理员");
            System.out.println("4. 返回主菜单");
            System.out.print("请选择操作: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    listAllUsers();
                    break;
                case "2":
                    updateUserPermission();
                    break;
                case "3":
                    promoteUserToAdmin();
                    break;
                case "4":
                    return;
                default:
                    System.out.println("无效选择，请重新输入。");
            }
        }
    }

    private static void listAllUsers() {
        System.out.println("\n--- 所有用户 ---");
        List<User> users = userService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("暂无用户数据。");
            return;
        }

        System.out.printf("%-10s %-20s %-15s %-15s\n",
                "ID", "用户名", "权限等级", "关联角色");
        System.out.println("------------------------------------------------");

        for (User user : users) {
            String characterInfo = "无";
            if (user.getCharacterId() != 0) {
                Character character = characterService.findCharacterById(user.getCharacterId());
                if (character != null) {
                    characterInfo = character.getName();
                } else {
                    characterInfo = "角色不存在";
                }
            }

            System.out.printf("%-10d %-20s %-15s %-15s\n",
                    user.getId(),
                    user.getUsername(),
                    user.isManager() ? "管理者" : "玩家",
                    characterInfo);
        }
    }

    private static void updateUserPermission() {
        System.out.println("\n--- 修改用户权限 ---");

        try {
            System.out.print("输入要修改的用户ID: ");
            long userId = Long.parseLong(scanner.nextLine());

            User targetUser = userService.getUserById(userId);
            if (targetUser == null) {
                System.out.println("未找到该用户！");
                return;
            }

            System.out.println("当前用户信息:");
            System.out.println("用户名: " + targetUser.getUsername());
            System.out.println("当前权限: " + (targetUser.isManager() ? "管理者" : "玩家"));

            System.out.println("选择新的权限等级:");
            System.out.println("1. 玩家");
            System.out.println("2. 管理者");
            System.out.print("请选择: ");

            String choice = scanner.nextLine().trim();
            int newPermissionLevel = "2".equals(choice) ? 1 : 0;

            boolean success = userService.updateUserPermission(userId, newPermissionLevel);
            if (success) {
                System.out.println("用户权限更新成功！");
            } else {
                System.out.println("用户权限更新失败！");
            }

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的用户ID！");
        } catch (Exception e) {
            System.out.println("修改用户权限失败: 系统错误 - " + e.getMessage());
        }
    }

    /**
     * 指定用户为管理员 - 新增功能
     * 支持用户名或用户ID搜索
     */
    private static void promoteUserToAdmin() {
        System.out.println("\n--- 指定用户为管理员 ---");

        try {
            System.out.print("请输入要指定为管理员的用户名或用户ID: ");
            String input = scanner.nextLine().trim();

            User targetUser;

            // 判断输入是用户ID还是用户名
            if (input.matches("\\d+")) {
                // 输入的是数字，按用户ID查找
                long userId = Long.parseLong(input);
                targetUser = userService.getUserById(userId);
            } else {
                // 输入的是字符串，按用户名查找
                targetUser = userService.getUserByUsername(input);
            }

            if (targetUser == null) {
                System.out.println("未找到该用户！");
                return;
            }

            // 检查是否已经是管理员
            if (targetUser.isManager()) {
                System.out.println("用户 " + targetUser.getUsername() + " 已经是管理员，无需重复指定。");
                return;
            }

            // 显示用户信息并确认
            System.out.println("\n找到用户:");
            System.out.println("用户ID: " + targetUser.getId());
            System.out.println("用户名: " + targetUser.getUsername());
            System.out.println("当前权限: 玩家");

            Character userCharacter;
            if (targetUser.getCharacterId() != 0) {
                userCharacter = characterService.findCharacterById(targetUser.getCharacterId());
                if (userCharacter != null) {
                    System.out.println("关联角色: " + userCharacter.getName());
                }
            }

            System.out.print("\n确认将该用户指定为管理员？(y/N): ");
            String confirmation = scanner.nextLine().trim();

            if ("y".equalsIgnoreCase(confirmation)) {
                boolean success = userService.updateUserPermission(targetUser.getId(), 1);
                if (success) {
                    System.out.println("✅ 用户 " + targetUser.getUsername() + " 已成功指定为管理员！");
                    System.out.println("该用户现在可以创建战役和管理其他用户。");
                } else {
                    System.out.println("❌ 指定管理员失败！");
                }
            } else {
                System.out.println("取消操作。");
            }

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的用户ID或用户名！");
        } catch (Exception e) {
            System.out.println("指定管理员失败: 系统错误 - " + e.getMessage());
        }
    }

    // 切换用户
    private static boolean switchUser() {
        System.out.println("\n--- 切换用户 ---");
        System.out.println("1. 重新登录");
        System.out.println("2. 返回主菜单");
        System.out.print("请选择: ");

        String choice = scanner.nextLine().trim();
        if ("1".equals(choice)) {
            userService.logout();
            return loginMenu();
        } else {
            return false;
        }
    }
}