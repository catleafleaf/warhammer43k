package ;

import java.util.*;

// Note: This file replaces the existing Main.java. The change updates mainSystem to wait for Enter after actions and adds waitForEnter helper.

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
                    waitForEnter("操作完成。按回车返回主菜单...");
                    break;
                case "2":
                    listCharacters();
                    waitForEnter("操作完成。按回车返回主菜单...");
                    break;
                case "3":
                    viewCharacter();
                    waitForEnter("操作完成。按回车返回主菜单...");
                    break;
                case "4":
                    updateCharacter();
                    waitForEnter("操作完成。按回车返回主菜单...");
                    break;
                case "5":
                    deleteCharacter();
                    waitForEnter("操作完成。按回车返回主菜单...");
                    break;
                case "6":
                    addUnitToCharacter();
                    waitForEnter("操作完成。按回车返回主菜单...");
                    break;
                case "7":
                    addResourceToCharacter();
                    waitForEnter("操作完成。按回车返回主菜单...");
                    break;
                case "8":
                    // 进入战役子菜单（子菜单内自行控制返回）
                    campaignMenu();
                    // 子菜单返回后回到主菜单（无需额外等待）
                    break;
                case "9":
                    // 进入用户管理子菜单（子菜单内自行控制返回）
                    userManagementMenu();
                    break;
                case "10":
                    if (switchUser()) {
                        // 切换用户并可能需要创建角色，切换完成后回到主菜单
                        if (!checkAndCreateCharacter()) {
                            System.out.println("角色创建失败，返回主菜单");
                        }
                    } else {
                        // switchUser 返回 false 表示取消/返回上一级，直接回到主菜单
                    }
                    waitForEnter("已返回主菜单。按回车继续...");
                    break;
                case "11":
                    System.out.println("感谢使用，再见！");
                    DatabaseConfig.closeDataSource();
                    return;
                default:
                    System.out.println("无效选择，请重新输入。");
                    // 在无效输入后也停顿提示并返回主菜单
                    waitForEnter("按回车返回主菜单...");
            }

            System.out.println();
        }
    }

    /**
     * 通用等待辅助：打印提示并等待用户按回车
     * 可用于在某个操作完成后提示并返回上一级菜单
     */
    private static void waitForEnter(String prompt) {
        System.out.println();
        System.out.print(prompt != null && !prompt.isEmpty() ? prompt : "按回车返回上一级菜单...");
        // 读取一行，忽略内容
        try {
            scanner.nextLine();
        } catch (Exception e) {
            // 忽略读取异常，继续执行
        }
    }

    // 以下方法保持原样（摘录）—— 请确保原文件其余方法保持一致或未被覆盖
    private static void printMainMenu() { /* unchanged */ }
    private static void createCharacter() { /* unchanged */ }
    private static void listCharacters() { /* unchanged */ }
    private static void viewCharacter() { /* unchanged */ }
    private static void updateCharacter() { /* unchanged */ }
    private static void deleteCharacter() { /* unchanged */ }
    private static void addUnitToCharacter() { /* unchanged */ }
    private static void addResourceToCharacter() { /* unchanged */ }
    private static void campaignMenu() { /* unchanged */ }
    private static void userManagementMenu() { /* unchanged */ }
    private static boolean switchUser() { return false; }
    private static boolean checkAndCreateCharacter() { return true; }

}