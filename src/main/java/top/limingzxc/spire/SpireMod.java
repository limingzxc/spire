package top.limingzxc.spire;

import top.limingzxc.spire.event.SpireEvents;
import top.limingzxc.spire.network.SpireNetwork;
import top.limingzxc.spire.relic.relics.SpireRelics;
import top.limingzxc.spire.relic.relics.StarterRelics;
import net.fabricmc.api.ModInitializer;
import net.xiaoyu233.fml.ModResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SpireCraft 主入口。
 * 杀戮尖塔风格 roguelike 爬塔模组 — MITE 版本。
 */
public class SpireMod implements ModInitializer {
    public static final String MOD_ID = "spirecraft";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("SpireCraft initializing...");
        ModResourceManager.addResourcePackDomain(MOD_ID);
        StarterRelics.registerAll();
        SpireRelics.registerAll();
        SpireEvents.register();
        SpireNetwork.register();
        LOGGER.info("SpireCraft initialized!");
    }
}