package service;

import model.Character;
import model.Faction;
import model.Unit;
import model.Resource;
import config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CharacterService {
    private static CharacterService instance;
    private final ConcurrentHashMap<Long, Character> memoryCache;
    private long nextCharacterId;

    private CharacterService() {
        memoryCache = new ConcurrentHashMap<>();
        initializeNextCharacterId();
        loadCharactersFromDatabase();
    }

    public static CharacterService getInstance() {
        if (instance == null) {
            instance = new CharacterService();
        }
        return instance;
    }

    private void initializeNextCharacterId() {
        String sql = "SELECT COALESCE(MAX(id), 10000000) + 1 AS next_id FROM characters";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                nextCharacterId = rs.getLong("next_id");
            } else {
                nextCharacterId = 10000001L;
            }
        } catch (SQLException e) {
            System.err.println("初始化角色ID失败: " + e.getMessage());
            nextCharacterId = 10000001L;
        }
    }

    private void loadCharactersFromDatabase() {
        String sql = "SELECT * FROM characters";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Character character = new Character(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("title"),
                        Faction.valueOf(rs.getString("faction")),
                        loadUnitsForCharacter(rs.getLong("id")),
                        loadResourcesForCharacter(rs.getLong("id"))
                );
                memoryCache.put(character.getId(), character);
            }
            System.out.println("从数据库加载 " + memoryCache.size() + " 个角色");
        } catch (SQLException e) {
            System.err.println("加载角色数据失败: " + e.getMessage());
        }
    }

    private List<Unit> loadUnitsForCharacter(long characterId) {
        List<Unit> units = new ArrayList<>();
        String sql = "SELECT * FROM units WHERE character_id = ?";
        try (ResultSet rs = DatabaseService.executeQuery(sql, characterId)) {
            while (rs.next()) {
                units.add(new Unit(
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getInt("count")
                ));
            }
        } catch (SQLException e) {
            System.err.println("加载单位数据失败: " + e.getMessage());
        }
        return units;
    }

    private List<Resource> loadResourcesForCharacter(long characterId) {
        List<Resource> resources = new ArrayList<>();
        String sql = "SELECT * FROM resources WHERE character_id = ?";
        try (ResultSet rs = DatabaseService.executeQuery(sql, characterId)) {
            while (rs.next()) {
                resources.add(new Resource(
                        rs.getString("name"),
                        rs.getInt("quantity")
                ));
            }
        } catch (SQLException e) {
            System.err.println("加载资源数据失败: " + e.getMessage());
        }
        return resources;
    }

    public Character createCharacter(String name, Faction faction) {
        long id = nextCharacterId++;
        Character character = new Character(id, name, "", faction, new ArrayList<>(), new ArrayList<>());

        // 保存到数据库
        String sql = "INSERT INTO characters (id, name, title, faction, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            DatabaseService.executeUpdate(sql,
                    character.getId(),
                    character.getName(),
                    character.getTitle(),
                    character.getFaction().name(),
                    character.getCreatedAt(),
                    character.getUpdatedAt()
            );

            // 保存到内存缓存
            memoryCache.put(character.getId(), character);

            return character;
        } catch (SQLException e) {
            throw new IllegalArgumentException("创建角色失败: " + e.getMessage());
        }
    }

    public List<Character> getAllCharacters() {
        return new ArrayList<>(memoryCache.values());
    }

    public Character findCharacterById(long id) {
        // 先从内存缓存获取
        Character character = memoryCache.get(id);
        if (character != null) {
            return character;
        }

        // 从数据库加载
        String sql = "SELECT * FROM characters WHERE id = ?";
        try (ResultSet rs = DatabaseService.executeQuery(sql, id)) {
            if (rs.next()) {
                character = new Character(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("title"),
                        Faction.valueOf(rs.getString("faction")),
                        loadUnitsForCharacter(id),
                        loadResourcesForCharacter(id)
                );
                memoryCache.put(id, character);
                return character;
            }
        } catch (SQLException e) {
            System.err.println("查找角色失败: " + e.getMessage());
        }
        return null;
    }

    public void updateCharacter(Character character) {
        String sql = "UPDATE characters SET name = ?, title = ?, faction = ?, updated_at = ? WHERE id = ?";
        try {
            DatabaseService.executeUpdate(sql,
                    character.getName(),
                    character.getTitle(),
                    character.getFaction().name(),
                    character.getUpdatedAt(),
                    character.getId()
            );

            // 更新内存缓存
            memoryCache.put(character.getId(), character);
        } catch (SQLException e) {
            throw new IllegalArgumentException("更新角色失败: " + e.getMessage());
        }
    }

    public boolean deleteCharacter(long id) {
        String sql = "DELETE FROM characters WHERE id = ?";
        try {
            DatabaseService.executeUpdate(sql, id);

            // 从内存缓存移除
            memoryCache.remove(id);

            return true;
        } catch (SQLException e) {
            System.err.println("删除角色失败: " + e.getMessage());
            return false;
        }
    }

    public void addUnitToCharacter(long characterId, Unit unit) {
        Character character = findCharacterById(characterId);
        if (character != null) {
            character.addUnit(unit);

            // 保存到数据库
            String sql = "INSERT INTO units (character_id, name, type, count) VALUES (?, ?, ?, ?)";
            try {
                DatabaseService.executeUpdate(sql,
                        characterId,
                        unit.getName(),
                        unit.getType(),
                        unit.getCount()
                );

                updateCharacter(character);
            } catch (SQLException e) {
                throw new IllegalArgumentException("添加单位失败: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("角色不存在");
        }
    }

    public void addResourceToCharacter(long characterId, Resource resource) {
        Character character = findCharacterById(characterId);
        if (character != null) {
            character.addResource(resource);

            // 保存到数据库
            String sql = "INSERT INTO resources (character_id, name, quantity) VALUES (?, ?, ?)";
            try {
                DatabaseService.executeUpdate(sql,
                        characterId,
                        resource.getName(),
                        resource.getQuantity()
                );

                updateCharacter(character);
            } catch (SQLException e) {
                throw new IllegalArgumentException("添加资源失败: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("角色不存在");
        }
    }
}