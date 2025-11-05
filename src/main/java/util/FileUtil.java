package util;

import model.Character;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件工具类
 * 负责所有文件IO操作，包括读写JSON文件、目录管理等
 * 使用Gson库进行JSON序列化和反序列化[citation:1]
 */
public class FileUtil {
    // Gson实例，配置为可读性强的格式[citation:10]
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()  // 格式化输出，便于阅读
            .serializeNulls()     // 序列化null值[citation:10]
            .create();

    /**
     * 确保目录存在，如果不存在则创建
     * @param directoryPath 目录路径
     */
    public static void ensureDirectoryExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("创建目录: " + directoryPath);
            }
        } catch (IOException e) {
            System.err.println("创建目录失败: " + directoryPath + " - " + e.getMessage());
        }
    }

    /**
     * 将角色保存为JSON文件[citation:1]
     * @param character 角色对象
     * @param filename 文件名
     */
    public static void saveCharacterToJson(Character character, String filename) {
        try (Writer writer = new FileWriter(filename)) {
            GSON.toJson(character, writer);
            System.out.println("角色保存成功: " + filename);
        } catch (IOException e) {
            System.err.println("保存角色到文件失败: " + filename + " - " + e.getMessage());
        }
    }

    /**
     * 从JSON文件加载角色[citation:1]
     * @param filename 文件名
     * @return 角色对象，如果加载失败返回null
     */
    public static Character loadCharacterFromJson(String filename) {
        try (Reader reader = new FileReader(filename)) {
            Character character = GSON.fromJson(reader, Character.class);
            if (character != null) {
                System.out.println("角色加载成功: " + filename);
            }
            return character;
        } catch (IOException e) {
            System.err.println("从文件加载角色失败: " + filename + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 从目录加载所有角色文件
     * @param directoryPath 目录路径
     * @return 角色列表
     */
    public static List<Character> loadAllCharactersFromDirectory(String directoryPath) {
        List<Character> characters = new ArrayList<>();
        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("目录不存在: " + directoryPath);
            return characters;
        }

        // 查找所有以"character_"开头，以".json"结尾的文件
        File[] files = directory.listFiles((dir, name) ->
                name.startsWith("character_") && name.endsWith(".json"));

        if (files != null) {
            System.out.println("找到 " + files.length + " 个角色文件");
            for (File file : files) {
                Character character = loadCharacterFromJson(file.getAbsolutePath());
                if (character != null) {
                    characters.add(character);
                }
            }
        } else {
            System.out.println("目录中没有找到角色文件");
        }

        return characters;
    }

    /**
     * 删除文件
     * @param filename 文件名
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filename) {
        File file = new File(filename);
        boolean deleted = file.delete();
        if (deleted) {
            System.out.println("文件删除成功: " + filename);
        } else {
            System.err.println("文件删除失败: " + filename);
        }
        return deleted;
    }

    /**
     * 导出角色到CSV文件
     * @param characters 角色列表
     * @param filename 文件名
     */
    public static void exportToCsv(List<Character> characters, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // 写入CSV表头
            writer.println("ID,Name,Title,Faction,Units,Resources,CreatedAt,UpdatedAt");

            // 写入数据行
            for (Character character : characters) {
                String line = String.format("%d,%s,%s,%s,%d,%d,%d,%d",
                        character.getId(),
                        escapeCsvField(character.getName()),
                        escapeCsvField(character.getTitle()),
                        character.getFaction().name(),
                        character.getUnits().size(),
                        character.getResources().size(),
                        character.getCreatedAt(),
                        character.getUpdatedAt());
                writer.println(line);
            }
            System.out.println("CSV导出成功: " + filename);
        } catch (IOException e) {
            System.err.println("导出CSV失败: " + filename + " - " + e.getMessage());
        }
    }

    /**
     * CSV字段转义处理
     * 处理包含逗号、引号或换行符的字段
     */
    private static String escapeCsvField(String field) {
        if (field == null) return "";
        // 如果字段包含逗号、换行或引号，需要用引号包围并转义内部引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}