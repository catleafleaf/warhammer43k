import java.util.*;
import java.util.stream.Collectors;

import config.DatabaseConfig;
import model.*;
import model.Character;
import service.*;

public class Main {
    private static final CharacterService characterService = CharacterService.getInstance();
    private static final CampaignService campaignService = CampaignService.getInstance();
    private static final DatabaseUserService userService = DatabaseUserService.getInstance();
    private static final Scanner scanner = new Scanner(System.in);

    private static final String SEPARATOR = "=========================================";
    private static final String SYSTEM_TITLE = "战锤40k角色与战役管理系统";
    private static final String SYSTEM_VERSION = "数据库持久化版本";
    private static final String PROMPT_RETURN_MAIN = "操作完成。按回车返回主菜单...";
    private static final String PROMPT_RETURN_MENU = "按回车返回上一级菜单...";
    private static final String EXIT_KEYWORD = "done";

    public static void main(String[] args) {
        if (!initializeSystem()) {
            return;
        }

        if (!handleUserAuthentication()) {
            DatabaseConfig.closeDataSource();
            return;
        }

        if (!handleCharacterSetup()) {
            DatabaseConfig.closeDataSource();
            return;
        }

        runMainSystem();
    }

    private static boolean initializeSystem() {
        System.out.println("正在测试数据库连接...");

        if (DatabaseConfig.testConnection()) {
            System.out.println("✅ 数据库连接成功！");
        } else {
            System.out.println("❌ 数据库连接失败！");
            printConnectionTroubleshooting();
            return false;
        }

        printSystemHeader();
        return true;
    }

    private static void printConnectionTroubleshooting() {
        System.out.println("请检查：");
        System.out.println("1. MySQL95 服务是否运行");
        System.out.println("2. 数据库配置是否正确");
        System.out.println("3. 数据库用户名密码是否正确");
    }

    private static void printSystemHeader() {
        System.out.println(SEPARATOR);
        System.out.println("    " + SYSTEM_TITLE);
        System.out.println("          " + SYSTEM_VERSION);
        System.out.println(SEPARATOR);
    }

    private static boolean handleUserAuthentication() {
        boolean shouldContinue = true;
        boolean loginSuccess = false;

        while (shouldContinue && !loginSuccess) {
            printLoginMenu();
            String choice = getInput("请选择操作: ");

            switch (choice) {
                case "1":
                    loginSuccess = handleLogin();
                    if (loginSuccess) {
                        shouldContinue = false;
                    }
                    break;
                case "2":
                    loginSuccess = handleRegistration();
                    if (loginSuccess) {
                        shouldContinue = false;
                    }
                    break;
                case "3":
                    System.out.println("系统退出");
                    return false;
                default:
                    System.out.println("无效选择，请重新输入。");
            }
        }

        return loginSuccess;
    }

    private static void printLoginMenu() {
        System.out.println("\n--- 用户登录 ---");
        System.out.println("1. 登录");
        System.out.println("2. 注册");
        System.out.println("3. 退出系统");
    }

    private static boolean handleLogin() {
        System.out.println("\n--- 用户登录 ---");

        String username = getInput("用户名: ");
        String password = getInput("密码: ");

        try {
            User user = userService.login(username, password);
            userService.setCurrentUser(user);
            printLoginSuccess(user);
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println("登录失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("登录失败: 系统错误 - " + e.getMessage());
            return false;
        }
    }

    private static void printLoginSuccess(User user) {
        String role = user.isManager() ? "管理者" : "玩家";
        System.out.println("登录成功！欢迎 " + user.getUsername() + " (" + role + ")");
    }

    private static boolean handleRegistration() {
        System.out.println("\n--- 用户注册 ---");

        String username = getInput("请输入用户名 (3-20位字母、数字、下划线): ");
        if (!userService.isUsernameAvailable(username)) {
            System.out.println("用户名已存在，请选择其他用户名");
            return false;
        }

        String password = getInput("请输入密码 (至少6位): ");
        String confirmPassword = getInput("确认密码: ");

        if (!password.equals(confirmPassword)) {
            System.out.println("两次输入的密码不一致");
            return false;
        }

        return registerUser(username, password);
    }

    private static boolean registerUser(String username, String password) {
        try {
            User user = userService.registerUser(username, password, 0, 0);
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

    private static boolean handleCharacterSetup() {
        User currentUser = userService.getCurrentUser();

        if (userHasLinkedCharacter(currentUser)) {
            return handleExistingCharacter(currentUser);
        } else {
            return handleNewCharacterCreation();
        }
    }

    private static boolean userHasLinkedCharacter(User user) {
        return userService.hasLinkedCharacter(user.getId());
    }

    private static boolean handleExistingCharacter(User user) {
        Character character = characterService.findCharacterById(user.getCharacterId());
        if (character != null) {
            System.out.println("欢迎回来，" + character.getName() + "!");
            return true;
        } else {
            System.out.println("关联的角色不存在，需要重新创建角色");
            return handleNewCharacterCreation();
        }
    }

    private static boolean handleNewCharacterCreation() {
        System.out.println("\n=== 角色创建 ===");
        System.out.println("欢迎新用户！在进入系统前，请先创建您的角色。");

        Character character = createCharacterInteractive();
        if (character == null) {
            return false;
        }

        return linkCharacterToCurrentUser(character);
    }

    private static boolean linkCharacterToCurrentUser(Character character) {
        User currentUser = userService.getCurrentUser();
        boolean linked = userService.linkCharacterToUser(currentUser.getId(), character.getId());

        if (linked) {
            System.out.println("角色创建成功！欢迎，" + character.getName() + "!");
            return true;
        } else {
            System.out.println("角色关联失败");
            return false;
        }
    }

    private static Character createCharacterInteractive() {
        System.out.println("\n--- 创建您的角色 ---");

        String name = getInput("输入角色名称: ");
        Faction faction = selectFaction();

        if (faction == null) {
            return null;
        }

        try {
            Character character = characterService.createCharacter(name, faction);
            handleInitialSetup(character);
            return character;
        } catch (IllegalArgumentException e) {
            System.out.println("创建角色失败: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("创建角色失败: 系统错误 - " + e.getMessage());
            return null;
        }
    }

    private static Faction selectFaction() {
        System.out.println("选择阵营:");
        Faction[] factions = Faction.values();

        for (int i = 0; i < factions.length; i++) {
            Faction faction = factions[i];
            System.out.printf("%d. %s - %s\n", i + 1, faction.getDisplayName(), faction.getBattleCry());
        }

        try {
            int factionIndex = Integer.parseInt(getInput("请选择阵营编号: ")) - 1;
            if (factionIndex < 0 || factionIndex >= factions.length) {
                System.out.println("无效的阵营编号！");
                return null;
            }
            return factions[factionIndex];
        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
            return null;
        }
    }

    private static void handleInitialSetup(Character character) {
        if (confirmAction("是否为角色添加初始单位？")) {
            addInitialUnits(character);
        }

        if (confirmAction("是否为角色添加初始资源？")) {
            addInitialResources(character);
        }
    }

    private static void addInitialUnits(Character character) {
        System.out.println("\n--- 添加初始单位 ---");
        System.out.println("输入 '" + EXIT_KEYWORD + "' 结束添加");

        boolean continueAdding = true;
        int unitCount = 0;

        while (continueAdding) {
            String unitName = getInput("输入单位名称: ");

            if (EXIT_KEYWORD.equalsIgnoreCase(unitName)) {
                continueAdding = false;
                continue;
            }

            boolean success = addSingleUnit(character.getId(), unitName);
            if (success) {
                unitCount++;
            }

            if (!confirmAction("是否继续添加单位？")) {
                continueAdding = false;
            }
        }

        System.out.println("单位添加完成，共添加 " + unitCount + " 个单位");
    }

    private static boolean addSingleUnit(long characterId, String unitName) {
        String unitType = getInput("输入单位类型: ");

        try {
            int count = Integer.parseInt(getInput("输入单位数量: "));
            Unit unit = new Unit(unitName, unitType, count);
            characterService.addUnitToCharacter(characterId, unit);
            System.out.println("单位添加成功！");
            return true;
        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println("添加单位失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("添加单位失败: 系统错误 - " + e.getMessage());
            return false;
        }
    }

    private static void addInitialResources(Character character) {
        System.out.println("\n--- 添加初始资源 ---");
        System.out.println("输入 '" + EXIT_KEYWORD + "' 结束添加");

        boolean continueAdding = true;
        int resourceCount = 0;

        while (continueAdding) {
            String resourceName = getInput("输入资源名称: ");

            if (EXIT_KEYWORD.equalsIgnoreCase(resourceName)) {
                continueAdding = false;
                continue;
            }

            boolean success = addSingleResource(character.getId(), resourceName);
            if (success) {
                resourceCount++;
            }

            if (!confirmAction("是否继续添加资源？")) {
                continueAdding = false;
            }
        }

        System.out.println("资源添加完成，共添加 " + resourceCount + " 种资源");
    }

    private static boolean addSingleResource(long characterId, String resourceName) {
        try {
            int quantity = Integer.parseInt(getInput("输入资源数量: "));
            Resource resource = new Resource(resourceName, quantity);
            characterService.addResourceToCharacter(characterId, resource);
            System.out.println("资源添加成功！");
            return true;
        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println("添加资源失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("添加资源失败: 系统错误 - " + e.getMessage());
            return false;
        }
    }

    private static void runMainSystem() {
        boolean systemRunning = true;

        while (systemRunning) {
            printMainMenu();
            String choice = getInput("请选择操作: ");

            systemRunning = processMainMenuChoice(choice);

            if (systemRunning) {
                System.out.println();
            }
        }

        DatabaseConfig.closeDataSource();
        System.out.println("系统已安全退出");
    }

    private static boolean processMainMenuChoice(String choice) {
        switch (choice) {
            case "1":
                createCharacter();
                waitForEnter(PROMPT_RETURN_MAIN);
                return true;
            case "2":
                listCharacters();
                waitForEnter(PROMPT_RETURN_MAIN);
                return true;
            case "3":
                viewCharacter();
                waitForEnter(PROMPT_RETURN_MAIN);
                return true;
            case "4":
                updateCharacter();
                waitForEnter(PROMPT_RETURN_MAIN);
                return true;
            case "5":
                deleteCharacter();
                waitForEnter(PROMPT_RETURN_MAIN);
                return true;
            case "6":
                addUnitToCharacterMenu();
                waitForEnter(PROMPT_RETURN_MAIN);
                return true;
            case "7":
                addResourceToCharacterMenu();
                waitForEnter(PROMPT_RETURN_MAIN);
                return true;
            case "8":
                campaignMenu();
                return true;
            case "9":
                userManagementMenu();
                return true;
            case "10":
                handleUserSwitch();
                waitForEnter(PROMPT_RETURN_MAIN);
                return true;
            case "11":
                System.out.println("感谢使用，再见！");
                return false;
            default:
                System.out.println("无效选择，请重新输入。");
                waitForEnter("按回车返回主菜单...");
                return true;
        }
    }

    private static void handleUserSwitch() {
        if (switchUser()) {
            if (!checkAndCreateCharacter()) {
                System.out.println("角色创建失败，返回主菜单");
            }
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
    }

    private static void createCharacter() {
        System.out.println("\n--- 创建新角色 ---");

        String name = getInput("输入角色名称: ");
        Faction faction = selectFaction();

        if (faction == null) {
            return;
        }

        try {
            Character character = characterService.createCharacter(name, faction);
            System.out.println("角色创建成功！");
            System.out.println("角色ID: " + character.getId());

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
        try {
            long id = Long.parseLong(getInput("输入要查看的角色ID: "));
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
        try {
            long id = Long.parseLong(getInput("输入要更新的角色ID: "));
            Character character = characterService.findCharacterById(id);

            if (character == null) {
                System.out.println("未找到该角色！");
                return;
            }

            System.out.println("当前角色信息:");
            printCharacterDetail(character);

            String newName = getInput("输入新名称 (直接回车保持原值): ");
            if (!newName.isEmpty()) {
                character.setName(newName);
            }

            String newTitle = getInput("输入新称号 (直接回车保持原值): ");
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
        try {
            long id = Long.parseLong(getInput("输入要删除的角色ID: "));
            Character character = characterService.findCharacterById(id);

            if (character == null) {
                System.out.println("未找到该角色！");
                return;
            }

            User currentUser = userService.getCurrentUser();
            if (currentUser != null && currentUser.getCharacterId() == id) {
                System.out.println("不能删除当前关联的角色！");
                return;
            }

            System.out.println("即将删除角色: " + character.getName());
            if (confirmAction("确认删除？")) {
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

    private static void addUnitToCharacterMenu() {
        try {
            long id = Long.parseLong(getInput("输入角色ID: "));
            Character character = characterService.findCharacterById(id);

            if (character == null) {
                System.out.println("未找到该角色！");
                return;
            }

            String unitName = getInput("输入单位名称: ");
            String unitType = getInput("输入单位类型: ");
            int count = Integer.parseInt(getInput("输入单位数量: "));

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

    private static void addResourceToCharacterMenu() {
        try {
            long id = Long.parseLong(getInput("输入角色ID: "));
            Character character = characterService.findCharacterById(id);

            if (character == null) {
                System.out.println("未找到该角色！");
                return;
            }

            String resourceName = getInput("输入资源名称: ");
            int quantity = Integer.parseInt(getInput("输入资源数量: "));

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

    private static void campaignMenu() {
        boolean inCampaignMenu = true;

        while (inCampaignMenu) {
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

            String choice = getInput("请选择操作: ");
            switch (choice) {
                case "1":
                    createCampaign();
                    waitForEnter(PROMPT_RETURN_MENU);
                    break;
                case "2":
                    startCampaign();
                    waitForEnter(PROMPT_RETURN_MENU);
                    break;
                case "3":
                    listCampaigns();
                    waitForEnter(PROMPT_RETURN_MENU);
                    break;
                case "4":
                    viewCampaign();
                    waitForEnter(PROMPT_RETURN_MENU);
                    break;
                case "5":
                    addEnemyBatchToCampaign();
                    waitForEnter(PROMPT_RETURN_MENU);
                    break;
                case "6":
                    inCampaignMenu = false;
                    break;
                default:
                    System.out.println("无效选择，请重新输入。");
            }
        }
    }

    private static void createCampaign() {
        System.out.println("\n--- 创建战役 ---");

        try {
            String name = getInput("输入战役名称: ");
            String description = getInput("输入战役描述: ");

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
            long campaignId = Long.parseLong(getInput("输入要开始的战役ID: "));
            Campaign campaign = campaignService.findCampaignById(campaignId);

            if (campaign == null) {
                System.out.println("未找到该战役！");
                return;
            }

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

            String input = getInput("输入要加入战役的角色ID（多个ID用逗号分隔）: ");
            List<Long> participantIds = Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

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
        try {
            long id = Long.parseLong(getInput("输入要查看的战役ID: "));
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
            long campaignId = Long.parseLong(getInput("输入战役ID: "));
            String batchName = getInput("输入批次名称: ");
            String batchDescription = getInput("输入批次描述: ");
            int spawnRound = Integer.parseInt(getInput("输入刷新回合: "));

            EnemyBatch batch = new EnemyBatch(batchName, batchDescription, spawnRound);

            addEnemyUnitsToBatch(batch);

            if (!batch.getEnemies().isEmpty()) {
                System.out.println("批次包含 " + batch.getEnemies().size() + " 个敌人单位");
                if (confirmAction("确认保存这个敌人批次？")) {
                    campaignService.addEnemyBatchToCampaign(campaignId, batch);
                    System.out.println("✅ 敌人批次添加成功！");
                } else {
                    System.out.println("❌ 取消保存敌人批次");
                }
            } else {
                System.out.println("批次为空，不进行保存");
            }

        } catch (NumberFormatException e) {
            System.out.println("请输入有效的数字！");
        } catch (IllegalArgumentException e) {
            System.out.println("添加敌人批次失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("添加敌人批次失败: 系统错误 - " + e.getMessage());
        }
    }

    private static void addEnemyUnitsToBatch(EnemyBatch batch) {
        System.out.println("\n--- 添加敌人单位 ---");

        boolean continueAdding = true;
        int unitCount = 0;

        while (continueAdding) {
            System.out.println("\n添加第 " + (unitCount + 1) + " 个敌人单位");

            Unit enemy = createEnemyUnitInteractive();

            if (enemy != null) {
                batch.addEnemy(enemy);
                unitCount++;
                System.out.println("✅ 敌人单位添加成功！");
            } else {
                System.out.println("❌ 敌人单位创建失败或取消");
            }

            if (!confirmAction("是否继续添加其他敌人单位？")) {
                continueAdding = false;
                System.out.println("已添加 " + unitCount + " 个敌人单位");
            }
        }
    }

    private static Unit createEnemyUnitInteractive() {
        try {
            String enemyName = getInputWithCancel("输入敌人单位名称 (输入'cancel'取消): ");
            if (isCancelInput(enemyName)) {
                return null;
            }

            String enemyType = getInputWithCancel("输入敌人单位类型 (输入'cancel'取消): ");
            if (isCancelInput(enemyType)) {
                return null;
            }

            Integer enemyCount = getIntegerInputWithCancel("输入敌人数量 (输入'cancel'取消): ");
            if (enemyCount == null) {
                return null;
            }

            Unit enemy = new Unit(enemyName, enemyType, enemyCount);
            System.out.println("即将创建: " + enemy);

            if (confirmAction("确认创建这个敌人单位？")) {
                return enemy;
            } else {
                return null;
            }

        } catch (Exception e) {
            System.out.println("创建敌人单位时发生错误: " + e.getMessage());
            return null;
        }
    }

    private static String getInputWithCancel(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        return input;
    }

    private static boolean isCancelInput(String input) {
        return "cancel".equalsIgnoreCase(input) || "退出".equals(input) || "quit".equalsIgnoreCase(input);
    }

    private static Integer getIntegerInputWithCancel(String prompt) {
        while (true) {
            String input = getInputWithCancel(prompt);

            if (isCancelInput(input)) {
                return null;
            }

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("❌ 请输入有效的数字！");
            }
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

    private static void userManagementMenu() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || !currentUser.isManager()) {
            System.out.println("权限不足：只有管理者可以访问用户管理功能");
            return;
        }

        boolean inUserManagement = true;

        while (inUserManagement) {
            System.out.println("\n--- 用户管理 ---");
            System.out.println("当前用户: " + currentUser.getUsername() + " (管理者)");
            System.out.println("1. 列出所有用户");
            System.out.println("2. 修改用户权限");
            System.out.println("3. 指定用户为管理员");
            System.out.println("4. 返回主菜单");

            String choice = getInput("请选择操作: ");
            switch (choice) {
                case "1":
                    listAllUsers();
                    waitForEnter(PROMPT_RETURN_MENU);
                    break;
                case "2":
                    updateUserPermission();
                    waitForEnter(PROMPT_RETURN_MENU);
                    break;
                case "3":
                    promoteUserToAdmin();
                    waitForEnter(PROMPT_RETURN_MENU);
                    break;
                case "4":
                    inUserManagement = false;
                    break;
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
            long userId = Long.parseLong(getInput("输入要修改的用户ID: "));
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
            String choice = getInput("请选择: ");

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

    private static void promoteUserToAdmin() {
        System.out.println("\n--- 指定用户为管理员 ---");

        try {
            String input = getInput("请输入要指定为管理员的用户名或用户ID: ");
            User targetUser;

            if (input.matches("\\d+")) {
                long userId = Long.parseLong(input);
                targetUser = userService.getUserById(userId);
            } else {
                targetUser = userService.getUserByUsername(input);
            }

            if (targetUser == null) {
                System.out.println("未找到该用户！");
                return;
            }

            if (targetUser.isManager()) {
                System.out.println("用户 " + targetUser.getUsername() + " 已经是管理员，无需重复指定。");
                return;
            }

            System.out.println("\n找到用户:");
            System.out.println("用户ID: " + targetUser.getId());
            System.out.println("用户名: " + targetUser.getUsername());
            System.out.println("当前权限: 玩家");

            if (targetUser.getCharacterId() != 0) {
                Character userCharacter = characterService.findCharacterById(targetUser.getCharacterId());
                if (userCharacter != null) {
                    System.out.println("关联角色: " + userCharacter.getName());
                }
            }

            if (confirmAction("确认将该用户指定为管理员？")) {
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

    private static boolean switchUser() {
        System.out.println("\n--- 切换用户 ---");
        System.out.println("1. 重新登录");
        System.out.println("2. 返回主菜单");

        String choice = getInput("请选择: ");
        if ("1".equals(choice)) {
            userService.logout();
            return handleUserAuthentication();
        } else {
            return false;
        }
    }

    private static boolean checkAndCreateCharacter() {
        User currentUser = userService.getCurrentUser();

        if (userHasLinkedCharacter(currentUser)) {
            Character character = characterService.findCharacterById(currentUser.getCharacterId());
            if (character != null) {
                System.out.println("欢迎回来，" + character.getName() + "!");
                return true;
            } else {
                System.out.println("关联的角色不存在，需要重新创建角色");
            }
        }

        System.out.println("\n=== 角色创建 ===");
        System.out.println("欢迎新用户！在进入系统前，请先创建您的角色。");

        Character character = createCharacterInteractive();
        if (character != null) {
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

    private static String getInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static boolean confirmAction(String prompt) {
        System.out.print(prompt + " (y/N): ");
        String choice = scanner.nextLine().trim();
        return "y".equalsIgnoreCase(choice);
    }

    private static void waitForEnter(String prompt) {
        System.out.println();
        System.out.print(prompt != null && !prompt.isEmpty() ? prompt : PROMPT_RETURN_MENU);
        try {
            scanner.nextLine();
        } catch (Exception e) {
        }
    }
}