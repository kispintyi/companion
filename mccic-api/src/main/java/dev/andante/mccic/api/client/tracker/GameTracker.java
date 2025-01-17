package dev.andante.mccic.api.client.tracker;

import dev.andante.mccic.api.client.event.MCCIChatEvent;
import dev.andante.mccic.api.client.event.MCCIGameEvents;
import dev.andante.mccic.api.client.util.ClientHelper;
import dev.andante.mccic.api.event.EventResult;
import dev.andante.mccic.api.game.Game;
import dev.andante.mccic.api.game.GameRegistry;
import dev.andante.mccic.api.game.GameState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Tracks game active data.
 */
@Environment(EnvType.CLIENT)
public class GameTracker {
    public static final GameTracker INSTANCE = new GameTracker();

    private static final String TIME_IDENTIFIER = ":";
    private static final String MCCI_PREFIX = "MCCI: ";

    private final MinecraftClient client;

    private GameState state;
    private Game game;
    private int time;

    public GameTracker() {
        this.client = MinecraftClient.getInstance();
        this.state = GameState.NONE;

        ClientTickEvents.END_WORLD_TICK.register(this::onWorldTick);
        MCCIChatEvent.EVENT.register(this::onChatMessage);
    }

    protected void onWorldTick(ClientWorld world) {
        if (!this.updateGame()) {
            this.game = null;
        }

        this.updateTime();
        this.updateState();
    }

    protected EventResult onChatMessage(MCCIChatEvent.Context context) {
        GameState oldState = this.state;

        String raw = context.getRaw();
        if (raw.startsWith("[")) {
            if (raw.endsWith(" started!")) {
                this.state = GameState.ACTIVE;
            } else if (raw.endsWith(" over!")) {
                this.state = raw.contains("Round") ? GameState.POST_ROUND : GameState.POST_GAME;
            } else if (raw.contains("you were eliminated")) {
                this.state = GameState.POST_ROUND_SELF;
            } else if (raw.contains("you finished the round and came")) {
                this.state = GameState.POST_ROUND_SELF;
            } else if (raw.contains("you didn't finish the round!")) {
                this.state = GameState.POST_ROUND_SELF;
            } else if (raw.contains("Team, you won Round")) {
                this.state = GameState.POST_ROUND;
            } else if (raw.contains("you won Round")) {
                this.state = GameState.POST_ROUND_SELF;
            } else if (raw.contains("Team, you lost Round")) {
                this.state = GameState.POST_ROUND;
            } else if (raw.contains("you lost Round")) {
                this.state = GameState.POST_ROUND_SELF;
            }
        }

        if (this.state != oldState) {
            MCCIGameEvents.STATE_UPDATE.invoker().onStateUpdate(this.state, oldState);
        }

        return EventResult.pass();
    }

    /**
     * Retrives the active game from the sidebar.
     * @return whether a game is present
     */
    protected boolean updateGame() {
        Scoreboard scoreboard = this.client.player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(1);
        if (objective != null) {
            String name = objective.getDisplayName().getString();
            if (name.contains(MCCI_PREFIX)) {
                String id = name.substring(MCCI_PREFIX.length());
                TypedActionResult<Game> result = GameRegistry.INSTANCE.fromScoreboard(id);
                Game game = !result.getResult().isAccepted() ? result.getValue() : null;
                if (game != this.game) {
                    MCCIGameEvents.GAME_CHANGE.invoker().onGameChange(game, this.game);
                    this.game = game;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Retrives the current time from the boss bar timer.
     */
    protected void updateTime() {
        if (this.game != null) {
            int lastTime = this.time;
            this.time = -1;

            ClientHelper.getBossBarStream()
                        .map(BossBar::getName)
                        .map(Text::getString)
                        .filter(name -> name.contains(TIME_IDENTIFIER))
                        .findAny()
                        .ifPresent(name -> {
                            int index = name.indexOf(TIME_IDENTIFIER);
                            String rawMins = name.substring(index - 2, index);
                            String rawSecs = name.substring(index + 1, index + 3);
                            int mins = Integer.parseInt(rawMins);
                            int secs = Integer.parseInt(rawSecs);
                            int time = (mins * 60) + secs;

                            if (time != lastTime) {
                                MCCIGameEvents.TIMER_UPDATE.invoker().onTimerUpdate(time, lastTime);
                            }

                            this.time = time;
                        });
        } else {
            if (this.time != -1) {
                MCCIGameEvents.TIMER_UPDATE.invoker().onTimerUpdate(-1, this.time);
            }

            this.time = -1;
        }
    }

    /**
     * Executes general hard-coded updates for the current inferred game state.
     */
    protected void updateState() {
        GameState oldState = this.state;

        if (this.game == null) {
            this.state = GameState.NONE;
        } else {
            if (this.state == GameState.NONE) {
                this.state = GameState.WAITING_FOR_GAME;
            }
        }

        if (this.state != oldState) {
            MCCIGameEvents.STATE_UPDATE.invoker().onStateUpdate(this.state, oldState);
        }
    }

    public Optional<Game> getGame() {
        return Optional.ofNullable(this.game);
    }

    public GameState getGameState() {
        return this.state;
    }

    public OptionalInt getTime() {
        return time == -1 ? OptionalInt.empty() : OptionalInt.of(this.time);
    }

    public boolean isInGame() {
        return this.game != null;
    }

    /**
     * Whether the client is connected to a server with
     * an IP address ending in <code>mccisland.net</code>.
     */
    public boolean isOnServer() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return true;
        }

        ServerInfo server = this.client.getCurrentServerEntry();
        if (server != null) {
            return server.address.endsWith("mccisland.net");
        }

        return false;
    }
}
