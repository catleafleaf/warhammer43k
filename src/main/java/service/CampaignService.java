package service;

import model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CampaignService {
    private static CampaignService instance;
    private final Map<Long, Campaign> campaigns;
    private final DatabaseUserService userService;

    private CampaignService() {
        campaigns = new ConcurrentHashMap<>();
        userService = DatabaseUserService.getInstance();
    }

    public static CampaignService getInstance() {
        if (instance == null) {
            instance = new CampaignService();
        }
        return instance;
    }

    // 创建战役（需要管理者权限）
    public Campaign createCampaign(String name, String description) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("请先登录");
        }

        if (!currentUser.isManager()) {
            throw new IllegalArgumentException("权限不足：只有管理者可以创建战役");
        }

        Campaign campaign = new Campaign(name, description, currentUser.getCharacterId());
        campaigns.put(campaign.getId(), campaign);
        return campaign;
    }

    // 开始战役
    public boolean startCampaign(long campaignId, List<Long> participantIds) {
        Campaign campaign = campaigns.get(campaignId);
        if (campaign == null) {
            throw new IllegalArgumentException("战役不存在");
        }

        // 检查权限：只有创建者可以开始战役
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("请先登录");
        }

        if (campaign.getCreatorId() != currentUser.getCharacterId()) {
            throw new IllegalArgumentException("只有战役创建者可以开始战役");
        }

        // 添加参与者
        for (Long participantId : participantIds) {
            campaign.addParticipant(participantId);
        }

        campaign.setStatus(CampaignStatus.STARTED);
        return true;
    }

    // 为战役添加敌人批次
    public void addEnemyBatchToCampaign(long campaignId, EnemyBatch batch) {
        Campaign campaign = campaigns.get(campaignId);
        if (campaign == null) {
            throw new IllegalArgumentException("战役不存在");
        }

        // 检查权限
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("请先登录");
        }

        if (campaign.getCreatorId() != currentUser.getCharacterId()) {
            throw new IllegalArgumentException("只有战役创建者可以添加敌人批次");
        }

        campaign.addEnemyBatch(batch);
    }

    // 获取所有战役
    public List<Campaign> getAllCampaigns() {
        return new ArrayList<>(campaigns.values());
    }

    // 根据ID查找战役
    public Campaign findCampaignById(long id) {
        return campaigns.get(id);
    }

    // 获取用户服务实例
    public DatabaseUserService getUserService() {
        return userService;
    }

    // 设置当前用户ID（已废弃，请使用UserService的setCurrentUserId）
    @Deprecated
    public void setCurrentUserId(long userId) {
        userService.setCurrentUserId(userId);
    }
}