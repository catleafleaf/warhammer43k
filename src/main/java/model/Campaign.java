package model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Campaign {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(20000000L);

    private long id;
    private String name;
    private String description;
    private long creatorId; // 创建者角色ID
    private CampaignStatus status;
    private List<Long> participantIds; // 参与角色ID列表
    private List<EnemyBatch> enemyBatches; // 敌人批次
    private int currentBatchIndex; // 当前批次索引

    public Campaign(String name, String description, long creatorId) {
        this.id = generateId();
        this.name = name;
        this.description = description;
        this.creatorId = creatorId;
        this.status = CampaignStatus.CREATED;
        this.participantIds = new ArrayList<>();
        this.enemyBatches = new ArrayList<>();
        this.currentBatchIndex = 0;
    }

    private long generateId() {
        long newId = ID_GENERATOR.incrementAndGet();
        if (newId > 29999999L) {
            ID_GENERATOR.set(20000000L);
            return ID_GENERATOR.incrementAndGet();
        }
        return newId;
    }

    // 添加参与者
    public boolean addParticipant(long characterId) {
        if (!participantIds.contains(characterId)) {
            participantIds.add(characterId);
            return true;
        }
        return false;
    }

    // 添加敌人批次
    public void addEnemyBatch(EnemyBatch batch) {
        enemyBatches.add(batch);
    }

    // 获取当前敌人批次
    public EnemyBatch getCurrentEnemyBatch() {
        if (enemyBatches.isEmpty() || currentBatchIndex >= enemyBatches.size()) {
            return null;
        }
        return enemyBatches.get(currentBatchIndex);
    }

    // 推进到下一批次
    public boolean nextBatch() {
        if (currentBatchIndex < enemyBatches.size() - 1) {
            currentBatchIndex++;
            return true;
        }
        return false;
    }

    // Getters and Setters
    public long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getCreatorId() { return creatorId; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public List<Long> getParticipantIds() { return new ArrayList<>(participantIds); }
    public List<EnemyBatch> getEnemyBatches() { return new ArrayList<>(enemyBatches); }
    public int getCurrentBatchIndex() { return currentBatchIndex; }
}