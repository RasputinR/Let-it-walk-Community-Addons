package dev.walkercompat.link;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.letitwalk.content.PilotControlsBlockEntity;
import dev.letitwalk.content.WalkerCoreBlockEntity;
import dev.letitwalk.walker.WalkerBrain;
import dev.letitwalk.walker.WalkerTracker;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** Reads the Pilot Controls' receive frequencies every server tick and drives the linked walker. */
public final class LinkDriver {
    /** Signal strengths at or below this are treated as off (stick drift). */
    private static final int DEADZONE = 1;
    /** Turn rate (rad/s) for a fully pushed analog turn; partial pushes scale down from this. */
    private static final double ANALOG_TURN_RATE = 0.7;

    private static final Set<PilotControlsBlockEntity> TRACKED =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private LinkDriver() {
    }

    public static void track(PilotControlsBlockEntity be) {
        TRACKED.add(be);
    }

    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<PilotControlsBlockEntity> snapshot;
        synchronized (TRACKED) {
            snapshot = new ArrayList<>(TRACKED);
        }
        for (PilotControlsBlockEntity be : snapshot) {
            if (be.isRemoved()) {
                TRACKED.remove(be);
            } else if (be.getLevel() == level) {
                tick(level, be);
            }
        }
    }

    private static void tick(ServerLevel level, PilotControlsBlockEntity be) {
        ReceiveHolder holder = (ReceiveHolder) be;
        ItemStackHandler freqs = holder.walkercompat$receive();
        LinkState state = holder.walkercompat$state();
        long now = level.getGameTime();

        int[] strength = new int[ReceiveAction.COUNT];
        boolean anyBound = false;
        for (ReceiveAction action : ReceiveAction.ALL) {
            Couple<Frequency> key = keyOf(freqs, action.ordinal());
            if (key != null) {
                anyBound = true;
                int s = strength(level, be.getBlockPos(), key);
                strength[action.ordinal()] = s > DEADZONE ? s : 0;
            }
        }
        if (!anyBound) {
            state.crab = false;
            if (state.linkKeys == 0 && !state.linkTurning) {
                return;
            }
        }

        WalkerCoreBlockEntity core = coreFor(level, be);

        // toggles and gear shifts react to the moment a signal turns on
        boolean crabOn = strength[ReceiveAction.CRAB.ordinal()] > 0;
        if (crabOn && !state.crabWasOn) {
            state.crab = !state.crab;
            tell(level, be, Component.translatable(state.crab ? "let_it_walk.controls.crab_on" : "let_it_walk.controls.crab_off"));
        }
        state.crabWasOn = crabOn;

        int topGear = core != null ? WalkerBrain.maxGear(core.getTheoreticalSpeed()) : WalkerBrain.GEAR_SPEED.length - 1;
        boolean upOn = strength[ReceiveAction.GEAR_UP.ordinal()] > 0;
        boolean downOn = strength[ReceiveAction.GEAR_DOWN.ordinal()] > 0;
        int wanted = state.gear;
        if (upOn && !state.gearUpWasOn) {
            wanted++;
        }
        if (downOn && !state.gearDownWasOn) {
            wanted--;
        }
        state.gearUpWasOn = upOn;
        state.gearDownWasOn = downOn;
        wanted = Math.max(0, Math.min(WalkerBrain.GEAR_SPEED.length - 1, wanted));
        if (wanted != state.gear) {
            if (wanted > topGear) {
                tell(level, be, Component.translatable("let_it_walk.gear.needs_rpm", wanted + 1, WalkerBrain.GEAR_RPM[wanted]));
            } else {
                state.gear = wanted;
                tell(level, be, Component.translatable("walker_controller_compat.gear", wanted + 1));
            }
        }
        state.gear = Math.min(state.gear, topGear);

        // held controls
        int keys = 0;
        for (ReceiveAction action : ReceiveAction.ALL) {
            if (action.bit != 0 && action != ReceiveAction.CRAB
                    && action != ReceiveAction.TURN_LEFT && action != ReceiveAction.TURN_RIGHT
                    && strength[action.ordinal()] > 0) {
                keys |= action.bit;
            }
        }
        if (state.crab) {
            keys |= ReceiveAction.CRAB.bit;
        }

        // turning: full signal = normal key turn, partial signal = smooth turn
        int left = strength[ReceiveAction.TURN_LEFT.ordinal()];
        int right = strength[ReceiveAction.TURN_RIGHT.ordinal()];
        double analogTurn = 0.0;
        if (state.crab) {
            if (left > 0) keys |= ReceiveAction.TURN_LEFT.bit;
            if (right > 0) keys |= ReceiveAction.TURN_RIGHT.bit;
        } else {
            double net = (left - right) / 15.0;
            if (net >= 1.0) {
                keys |= ReceiveAction.TURN_LEFT.bit;
            } else if (net <= -1.0) {
                keys |= ReceiveAction.TURN_RIGHT.bit;
            } else {
                analogTurn = net;
            }
        }

        boolean wasActive = state.linkKeys != 0 || state.linkTurning;
        state.linkKeys = keys;
        state.linkTurning = analogTurn != 0.0;
        boolean active = keys != 0 || state.linkTurning;

        if (core == null || (!active && !wasActive)) {
            return;
        }
        // keep the walker fed every tick while a signal is on, and once more when it stops
        boolean pilot = state.pilotRecent(now);
        core.receiveInput(state.merge(state.pilotKeys, pilot), now);
        if (analogTurn != 0.0) {
            // Let it Walk!'s mouse-turn path: rate = mouseTurn / 0.05 per tick
            core.receiveTurn(analogTurn * ANALOG_TURN_RATE * 0.05);
        }
    }

    @Nullable
    private static Couple<Frequency> keyOf(ItemStackHandler freqs, int action) {
        Frequency a = Frequency.of(freqs.getStackInSlot(action * 2));
        Frequency b = Frequency.of(freqs.getStackInSlot(action * 2 + 1));
        if (a == Frequency.EMPTY && b == Frequency.EMPTY) {
            return null;
        }
        return Couple.create(a, b);
    }

    /** Strongest signal on this frequency within link range, measured across Sable sub-levels. */
    private static int strength(ServerLevel level, BlockPos pos, Couple<Frequency> key) {
        Set<IRedstoneLinkable> network = Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level).get(key);
        if (network == null || network.isEmpty()) {
            return 0;
        }
        int range = AllConfigs.server().logistics.linkRange.get();
        double rangeSq = (double) range * range;
        Vec3 here = Vec3.atCenterOf(pos);
        int best = 0;
        for (IRedstoneLinkable link : new ArrayList<>(network)) {
            if (!link.isAlive()) {
                continue;
            }
            int s = link.getTransmittedStrength();
            if (s > best && Sable.HELPER.distanceSquaredWithSubLevels(level, here, Vec3.atCenterOf(link.getLocation())) < rangeSq) {
                best = s;
                if (best >= 15) {
                    break;
                }
            }
        }
        return best;
    }

    /** Same lookup Let it Walk! uses for the pilot: a linked walker, or the walker these controls sit on. */
    @Nullable
    public static WalkerCoreBlockEntity coreFor(ServerLevel level, PilotControlsBlockEntity be) {
        WalkerCoreBlockEntity core;
        if (be.linkedWalker != null) {
            core = WalkerCoreBlockEntity.linked(level.getServer(), be.linkedWalker);
        } else {
            SubLevel sub = Sable.HELPER.getContaining(level, be.getBlockPos());
            BlockPos corePos = sub != null ? WalkerTracker.coreOf(sub) : null;
            core = corePos != null && level.getBlockEntity(corePos) instanceof WalkerCoreBlockEntity c ? c : null;
        }
        return core != null && core.isAssembled() ? core : null;
    }

    /** Action-bar message to players near the controls. */
    private static void tell(ServerLevel level, PilotControlsBlockEntity be, Component message) {
        Vec3 here = Vec3.atCenterOf(be.getBlockPos());
        for (ServerPlayer player : level.players()) {
            if (Sable.HELPER.distanceSquaredWithSubLevels(level, player.position(), here) < 32 * 32) {
                player.displayClientMessage(message, true);
            }
        }
    }
}
