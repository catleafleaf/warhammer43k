package model;

import java.util.ArrayList;
import java.util.List;

public class EnemyBatch {
    private String name;
    private String description;
    private List<Unit> enemies;
    private int spawnRound; // 在第几回合刷新

    public EnemyBatch(String name, String description, int spawnRound) {
        this.name = name;
        this.description = description;
        this.spawnRound = spawnRound;
        this.enemies = new ArrayList<>();
    }

    public void addEnemy(Unit enemy) {
        enemies.add(enemy);
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Unit> getEnemies() { return new ArrayList<>(enemies); }
    public int getSpawnRound() { return spawnRound; }
    public void setSpawnRound(int spawnRound) { this.spawnRound = spawnRound; }
}