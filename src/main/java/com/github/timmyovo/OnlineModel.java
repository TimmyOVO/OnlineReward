package com.github.timmyovo;

import com.github.skystardust.ultracore.core.database.models.UltraCoreBaseModel;
import io.ebean.EbeanServer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.UUID;

@Entity
@Table(name = "p_online")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OnlineModel extends UltraCoreBaseModel {
    @Id
    private UUID id;
    private UUID player;
    private long time;
    private int onlineTime;
    private String rewards;

    public OnlineModel(UUID player, long time, int onlineTime) {
        this.id = UUID.randomUUID();
        this.player = player;
        this.time = time;
        this.onlineTime = onlineTime;
        this.rewards = "";
    }

    @Override
    public EbeanServer modelEbeanServer() {
        return OnlineReward.getInstance().getDatabaseManager().getEbeanServer();
    }

    public void increaseOnlineTime() {
        this.onlineTime++;
        update();
    }

    public void markAsRewarded(int tier) {
        if (rewards == null || rewards.isEmpty()) {
            this.rewards = Integer.toString(tier);
        } else {
            this.rewards += "#" + tier;
        }
        update();
    }

    public boolean isTierRewarded(int tier) {
        if (rewards == null || rewards.isEmpty()) {
            return false;
        }
        return Arrays.stream(rewards.split("#"))
                .mapToInt(Integer::parseInt)
                .anyMatch(i -> i == tier);
    }
}
