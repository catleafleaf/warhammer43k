package model;

/**
 * 物资资源类
 */
public class Resource {
    private final String name;
    private String description;
    private int quantity;
    private int quality;

    public Resource(String name, int quantity) {
        this.name = (name != null) ? name.trim() : "未命名资源";
        this.quantity = Math.max(0, quantity);
        this.description = "";
        this.quality = 50;
    }

    public Resource(Resource other) {
        this.name = other.name;
        this.description = other.description;
        this.quantity = other.quantity;
        this.quality = other.quality;
    }

    public void merge(Resource other) {
        if (this.name.equalsIgnoreCase(other.name)) {
            this.quantity += other.quantity;
            this.quality = Math.max(this.quality, other.quality);
        }
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = Math.max(0, quantity); }
    public int getQuality() { return quality; }
    public void setQuality(int quality) { this.quality = Math.max(1, Math.min(100, quality)); }

    @Override
    public String toString() {
        return String.format("%s x%d (品质:%d)", name, quantity, quality);
    }
}