package model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Character {
    // 使用原子长整型确保ID生成的线程安全性，避免在多线程环境下ID冲突
    private static final AtomicLong ID_GENERATOR = new AtomicLong(10000000L);

    // 基础信息字段
    private long id;                    // 8位数字ID
    private String name;                // 角色名称
    private String title;               // 角色称号
    private Faction faction;            // 阵营枚举

    // 麾下单位集合
    private List<Unit> units;

    // 麾下物资集合
    private List<Resource> resources;

    // 时间戳字段
    private long createdAt;
    private long updatedAt;

    /**
     * 全参数构造函数 - 用于从持久化存储重建对象
     */
    public Character(long id, String name, String title, Faction faction,
                     List<Unit> units, List<Resource> resources) {
        this.id = (id == 0) ? generateId() : id;
        validateName(name);
        this.name = name.trim();
        this.title = (title != null) ? title.trim() : "";
        this.faction = (faction != null) ? faction : Faction.IMPERIUM;
        this.units = (units != null) ? new ArrayList<>(units) : new ArrayList<>();
        this.resources = (resources != null) ? new ArrayList<>(resources) : new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    /**
     * 简化构造函数 - 用于快速创建新角色
     */
    public Character(String name, Faction faction) {
        this(0, name, "", faction, null, null);
    }

    /**
     * 生成唯一8位数字ID
     */
    private long generateId() {
        long newId = ID_GENERATOR.incrementAndGet();
        if (newId > 99999999L) {
            ID_GENERATOR.set(10000000L);
            return ID_GENERATOR.incrementAndGet();
        }
        return newId;
    }

    /**
     * 名称验证方法
     */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }

        String trimmedName = name.trim();
        if (trimmedName.length() < 2 || trimmedName.length() > 50) {
            throw new IllegalArgumentException("角色名称长度必须在2-50个字符之间");
        }

        if (!trimmedName.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5\\s\\-.]+$")) {
            throw new IllegalArgumentException("角色名称包含非法字符");
        }
    }

    // ==================== 业务方法 ====================

    public void addUnit(Unit unit) {
        if (unit != null) {
            Unit unitCopy = new Unit(unit.getName(), unit.getType(), unit.getCount());
            this.units.add(unitCopy);
            updateTimestamp();
        }
    }

    public boolean removeUnit(String unitName) {
        boolean removed = units.removeIf(unit -> unit.getName().equalsIgnoreCase(unitName));
        if (removed) {
            updateTimestamp();
        }
        return removed;
    }

    public void addResource(Resource resource) {
        if (resource != null) {
            resources.stream()
                    .filter(r -> r.getName().equalsIgnoreCase(resource.getName()))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> existing.merge(resource),
                            () -> resources.add(new Resource(resource))
                    );
            updateTimestamp();
        }
    }

    public boolean updateResourceQuantity(String resourceName, int newQuantity) {
        return resources.stream()
                .filter(r -> r.getName().equalsIgnoreCase(resourceName))
                .findFirst()
                .map(r -> {
                    r.setQuantity(newQuantity);
                    updateTimestamp();
                    return true;
                })
                .orElse(false);
    }

    // ==================== 时间戳管理 ====================

    private void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    public void markUpdated() {
        updateTimestamp();
    }

    // ==================== Getter和Setter ====================

    public long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) {
        validateName(name);
        this.name = name.trim();
        updateTimestamp();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = (title != null) ? title.trim() : "";
        updateTimestamp();
    }

    public Faction getFaction() { return faction; }
    public void setFaction(Faction faction) {
        this.faction = faction;
        updateTimestamp();
    }

    public List<Unit> getUnits() {
        return new ArrayList<>(units);
    }

    public List<Resource> getResources() {
        return new ArrayList<>(resources);
    }

    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    // ==================== 工具方法 ====================

    @Override
    public String toString() {
        return String.format("Character{id=%d, name='%s', title='%s', faction=%s, units=%d, resources=%d}",
                id, name, title, faction, units.size(), resources.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Character character = (Character) o;
        return id == character.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}