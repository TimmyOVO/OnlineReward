package com.github.timmyovo;

import com.github.skystardust.ultracore.bukkit.commands.MainCommandSpec;
import com.github.skystardust.ultracore.core.PluginInstance;
import com.github.skystardust.ultracore.core.configuration.ConfigurationManager;
import com.github.skystardust.ultracore.core.database.newgen.DatabaseManager;
import com.github.skystardust.ultracore.core.exceptions.ConfigurationException;
import com.github.skystardust.ultracore.core.exceptions.DatabaseInitException;
import com.google.common.collect.ImmutableBiMap;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Getter
public class OnlineReward extends JavaPlugin implements PluginInstance {
    private static OnlineReward instance;
    private DatabaseManager databaseManager;

    private GeneralConfiguration generalConfiguration;

    public static OnlineReward getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        new ConfigurationManager(this)
                .registerConfiguration("generalConfiguration", () -> new GeneralConfiguration(ImmutableBiMap.of(20, Arrays.asList("*kill all"))))
                .init(getClass(), this)
                .start();
        try {
            this.databaseManager = DatabaseManager.newBuilder()
                    .withName("onlinereward")
                    .withOwnerPlugin(this)
                    .withSqlConfiguration(DatabaseManager.setupDatabase(this))
                    .withModelClass(Arrays.asList(OnlineModel.class))
                    .build()
                    .openConnection();
        } catch (DatabaseInitException | ConfigurationException e) {
            getLogger().warning("Database failed to connect because " + e.getLocalizedMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            newTask(() -> Bukkit.getOnlinePlayers()
                    .forEach(player -> getTodayRecord(player).increaseOnlineTime()));
        }, 0L, 20 * 60);
        MainCommandSpec.newBuilder()
                .addAlias("onlinereward")
                .addAlias("ol")
                .withCommandSpecExecutor((commandSender, args) -> {
                    if (!(commandSender instanceof Player)) {
                        return false;
                    }
                    if (args.length < 1) {
                        commandSender.sendMessage("/ol tier");
                        return true;
                    }
                    int tier = 0;
                    try {
                        tier = Integer.parseInt(args[0]);
                    } catch (NumberFormatException ignored) {

                    }
                    Player player = (Player) commandSender;
                    int finalTier = tier;
                    newTask(() -> {
                        if (hasReachTier(player, finalTier)) {
                            if (isRewarded(player, finalTier)) {
                                commandSender.sendMessage("已经领取过了");
                                return;
                            }
                            List<String> commands = getGeneralConfiguration().getCommandsMap()
                                    .get(finalTier);
                            if (commands == null) {
                                return;
                            }
                            commandSender.sendMessage("成功领取");
                            markAsRewarded(player, finalTier);
                            commands.forEach(command -> {
                                command = PlaceholderAPI.setPlaceholders(player, command);
                                if (command.startsWith("*")) {
                                    command = command.substring(1);
                                    getServer().dispatchCommand(getServer().getConsoleSender(), command);
                                    return;
                                }
                                getServer().dispatchCommand(player, command);
                            });
                        } else {
                            commandSender.sendMessage("在线时间不足");
                        }
                    });
                    return true;
                })
                .build()
                .register();
    }

    private void newTask(Runnable r) {
        new Thread(r).start();
    }

    private boolean isRewarded(Player player, int tier) {
        return getTodayRecord(player).isTierRewarded(tier);
    }

    private void markAsRewarded(Player player, int tier) {
        getTodayRecord(player).markAsRewarded(tier);
    }

    private boolean hasReachTier(Player player, int tier) {
        return getTodayRecord(player).getOnlineTime() >= tier;
    }

    private OnlineModel getTodayRecord(Player player) {
        if (!hasRecordToday(player)) {
            OnlineModel onlineModel = new OnlineModel(player.getUniqueId(), System.currentTimeMillis(), 0);
            onlineModel.save();
            return onlineModel;
        }
        return this.databaseManager
                .getEbeanServer()
                .find(OnlineModel.class)
                .where()
                .eq("player", player.getUniqueId())
                .findList()
                .stream()
                .filter(onlineModel -> onlineModel.getPlayer().equals(player.getUniqueId()))
                .filter(onlineModel -> isToday(onlineModel.getTime()))
                .findFirst().orElseThrow(ImpossibleException::new);
    }

    private boolean hasRecordToday(Player player) {
        return this.databaseManager
                .getEbeanServer()
                .find(OnlineModel.class)
                .where()
                .eq("player", player.getUniqueId())
                .findList()
                .stream()
                .filter(onlineModel -> onlineModel.getPlayer().equals(player.getUniqueId()))
                .anyMatch(onlineModel -> isToday(onlineModel.getTime()));
    }

    private boolean isToday(long time) {
        Calendar now = Calendar.getInstance();
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date(time));
        return (instance.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) && (instance.get(Calendar.YEAR) == now.get(Calendar.YEAR));
    }

    @Override
    public Logger getPluginLogger() {
        return getLogger();
    }
}
