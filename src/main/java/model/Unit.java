package model;

/**
 * 麾下单位类
 */
public class Unit {
    private final String name;
    private final String type;
    private int count;
    private int morale;
    private int experience;

    public Unit(String name, String type, int count) {
        this.name = (name != null) ? name.trim() : "未命名单位";
        this.type = (type != null) ? type.trim() : "步兵";
        this.count = Math.max(0, count);
        this.morale = 100;
        this.experience = 0;
    }

    public Unit(Unit other) {
        this.name = other.name;
        this.type = other.type;
        this.count = other.count;
        this.morale = other.morale;
        this.experience = other.experience;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = Math.max(0, count); }
    public int getMorale() { return morale; }
    public void setMorale(int morale) { this.morale = Math.max(0, Math.min(100, morale)); }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }

    public void gainExperience(int exp) {
        this.experience += Math.max(0, exp);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) x%d", name, type, count);
    }
}