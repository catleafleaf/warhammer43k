package model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Character {
    // 使用原子长整型确保ID生成的线程安全性，避免在多线程环境下ID冲突
    private static final AtomicLong ID_GENERATOR = new AtomicLong(10000000L);

    // 基础信息字段 - 使用final确保一旦设置就不会被意外修改
    private final long id;                    // 8位数字ID，使用long类型确保数值范围
    private String name;                      // 角色名称
    private String title;                     // 角色称号
    private Faction faction;                  // 阵营枚举，避免魔法数字

    // 麾下单位集合 - 使用List保持顺序，ArrayList提供快速随机访问
    private List<Unit> units;

    // 麾下物资集合 - 使用泛型确保类型安全
    private List<Resource> resources;

    // 时间戳字段 - 用于追踪数据变更历史
    private long createdAt;
    private long updatedAt;

    /**
     * 全参数构造函数 - 用于从持久化存储重建对象
     * @param id 8位数字ID，如果为0则自动生成
     * @param name 角色名称，不能为空或空白
     * @param title 角色称号
     * @param faction 阵营枚举
     * @param units 麾下单位列表
     * @param resources 麾下物资列表
     *
     * 设计说明：这个构造函数主要用于数据恢复场景，确保从文件或数据库加载时能完整重建对象状态
     */
    public Character(long id, String name, String title, Faction faction,
                     List<Unit> units, List<Resource> resources) {
        // ID处理逻辑：如果传入ID为0，则自动生成；否则使用传入ID
        // 这样可以同时支持新角色创建和现有角色加载
        this.id = (id == 0) ? generateId() : id;

        // 参数验证 - 使用专门的验证方法提高代码可读性和可维护性
        validateName(name);
        this.name = name.trim();
        this.title = (title != null) ? title.trim() : "";
        this.faction = (faction != null) ? faction : Faction.IMPERIUM;

        // 集合初始化 - 防御性拷贝，防止外部修改影响内部状态
        this.units = (units != null) ? new ArrayList<>(units) : new ArrayList<>();
        this.resources = (resources != null) ? new ArrayList<>(resources) : new ArrayList<>();

        // 时间戳记录 - 用于数据同步和冲突检测
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    /**
     * 简化构造函数 - 用于快速创建新角色
     * 适合大多数新建角色的场景，减少参数传递的复杂性
     */
    public Character(String name, Faction faction) {
        this(0, name, "", faction, null, null);
    }

    /**
     * 生成唯一8位数字ID
     * 使用原子操作确保线程安全，避免在多线程环境下ID重复
     * @return 8位数字ID，范围10000000-99999999
     */
    private long generateId() {
        long newId = ID_GENERATOR.incrementAndGet();
        // ID范围检查，确保始终是8位数
        if (newId > 99999999L) {
            // 如果超过最大值，重置生成器（在实际生产环境中应该记录警告日志）
            ID_GENERATOR.set(10000000L);
            return ID_GENERATOR.incrementAndGet();
        }
        return newId;
    }

    /**
     * 名称验证方法
     * 验证规则：
     * 1. 不能为null
     * 2. 不能为空字符串或纯空白字符
     * 3. 长度限制为2-50个字符
     * 4. 不能包含非法字符
     */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }

        String trimmedName = name.trim();
        if (trimmedName.length() < 2 || trimmedName.length() > 50) {
            throw new IllegalArgumentException("角色名称长度必须在2-50个字符之间");
        }

        // 简单字符验证 - 可以根据需要扩展
        if (!trimmedName.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5\\s\\-\\.]+$")) {
            throw new IllegalArgumentException("角色名称包含非法字符");
        }
    }

    // ==================== 业务方法 ====================

    /**
     * 添加单位到麾下
     * 设计考虑：使用防御性拷贝确保传入的Unit对象不会被外部修改
     * @param unit 要添加的单位，会创建副本以避免引用问题
     */
    public void addUnit(Unit unit) {
        if (unit != null) {
            // 创建防御性拷贝
            Unit unitCopy = new Unit(unit.getName(), unit.getType(), unit.getCount());
            this.units.add(unitCopy);
            updateTimestamp();
        }
    }

    /**
     * 移除指定单位
     * 使用流操作提高代码可读性，特别是当Unit类实现equals/hashCode时
     * @param unitName 要移除的单位名称
     * @return 是否成功移除
     */
    public boolean removeUnit(String unitName) {
        boolean removed = units.removeIf(unit -> unit.getName().equalsIgnoreCase(unitName));
        if (removed) {
            updateTimestamp();
        }
        return removed;
    }

    /**
     * 添加资源
     * 如果资源已存在，则合并数量；否则添加新资源
     * @param resource 要添加的资源
     */
    public void addResource(Resource resource) {
        if (resource != null) {
            // 检查是否已存在同名资源
            resources.stream()
                    .filter(r -> r.getName().equalsIgnoreCase(resource.getName()))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> existing.merge(resource), // 合并数量
                            () -> resources.add(new Resource(resource)) // 添加新资源
                    );
            updateTimestamp();
        }
    }

    /**
     * 更新资源数量
     * @param resourceName 资源名称
     * @param newQuantity 新数量
     * @return 是否成功更新
     */
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

    /**
     * 更新修改时间戳
     * 在每次数据变更时调用，确保数据版本控制
     */
    private void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 标记数据变更 - 供外部调用的公共方法
     * 当通过setter方法修改字段时应该调用此方法
     */
    public void markUpdated() {
        updateTimestamp();
    }

    // ==================== Getter和Setter ====================

    // ID是只读的，创建后不能修改
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

    // 返回不可修改的集合视图，防止外部直接修改内部集合
    public List<Unit> getUnits() {
        return new ArrayList<>(units);
    }

    public List<Resource> getResources() {
        return new ArrayList<>(resources);
    }

    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    // ==================== 工具方法 ====================

    /**
     * 转换为字符串表示，主要用于调试
     * 生产环境中应该使用专门的序列化方法
     */
    @Override
    public String toString() {
        return String.format("Character{id=%d, name='%s', title='%s', faction=%s, units=%d, resources=%d}",
                id, name, title, faction, units.size(), resources.size());
    }

    /**
     * 相等性判断基于ID，因为ID是唯一的
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Character character = (Character) o;
        return id == character.id;
    }

    /**
     * 哈希码基于ID，确保在基于哈希的集合中正确工作
     */
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}