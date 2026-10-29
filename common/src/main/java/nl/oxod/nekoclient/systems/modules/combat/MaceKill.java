/*
 * Original by Trouser-Streak (https://github.com/etianl/Trouser-Streak)
 */

package nl.oxod.nekoclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.oxod.nekoclient.events.packets.PacketEvent;
import nl.oxod.nekoclient.mixininterface.IServerboundMovePlayerPacket;
import nl.oxod.nekoclient.settings.BoolSetting;
import nl.oxod.nekoclient.settings.DoubleSetting;
import nl.oxod.nekoclient.settings.IntSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;

public class MaceKill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTotem = settings.createGroup("Totem Bypass");

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packetDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-when-blocked")
        .description("Does not send movement packets if the attack was blocked.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
        .name("fall-height")
        .defaultValue(22)
        .min(1)
        .max(169)
        .sliderRange(1, 169)
        .build()
    );

    private final Setting<Integer> paperPackets = sgGeneral.add(new IntSetting.Builder()
        .name("spam-packets")
        .defaultValue(4)
        .min(1)
        .sliderRange(1, 17)
        .build()
    );

    private final Setting<Boolean> attackSpam = sgTotem.add(new BoolSetting.Builder()
        .name("totem-bypass")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> attacks = sgTotem.add(new IntSetting.Builder()
        .name("attacks")
        .defaultValue(3)
        .min(0)
        .sliderRange(1, 3)
        .build()
    );

    private final Setting<Integer> increase = sgTotem.add(new IntSetting.Builder()
        .name("height-increase")
        .defaultValue(9)
        .min(1)
        .max(100)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> useOffset = sgGeneral.add(new BoolSetting.Builder()
        .name("use-offset")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> offsetHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-offset")
        .defaultValue(0.05)
        .min(0)
        .sliderMax(0.99)
        .visible(useOffset::get)
        .build()
    );

    private final Setting<Double> offsetY = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-offset")
        .defaultValue(0.01)
        .min(0)
        .sliderMax(0.99)
        .visible(useOffset::get)
        .build()
    );

    private Vec3 previousPos;
    private boolean sendingAttacks = false;

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    private final Map<Vec3, Boolean> positionCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Vec3, Boolean> eldest) {
            return size() > 256;
        }
    };

    public MaceKill() {
        super(Categories.Combat, "mace-kill", "Mace smash exploit - credits to Trouser-Streak");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (sendingAttacks) return;
        if (mc.player == null || mc.level == null) return;
        if (mc.player.isPassenger() || mc.player.getMainHandItem().getItem() != Items.MACE) return;
        if (!(event.packet instanceof ServerboundAttackPacket packet)) return;

        Entity targetEntity = null;
        try {
            java.lang.reflect.Field f = ServerboundAttackPacket.class.getDeclaredField("target");
            f.setAccessible(true);
            Object o = f.get(packet);
            if (o instanceof net.minecraft.world.entity.Entity e) targetEntity = e;
        } catch (Exception ignored) {}
        if (targetEntity == null) {
            try {
                java.lang.reflect.Field f = ServerboundAttackPacket.class.getDeclaredField("entityId");
                f.setAccessible(true);
                int id = f.getInt(packet);
                targetEntity = mc.level.getEntity(id);
            } catch (Exception ignored) {}
        }
        if (!(targetEntity instanceof LivingEntity living)) return;
        if (packetDisable.get() && (living.isBlocking() || living.isInvulnerable())) return;
        if (!living.isAlive()) return;

        int baseBlocks = fallHeight.get();
        event.cancel();

        previousPos = mc.player.position();
        int currentHeight = baseBlocks;
        int attackCount = attacks.get();
        if (!attackSpam.get()) attackCount = 1;

        for (int i = 0; i < paperPackets.get(); i++) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision));
        }

        try {
            for (int i = 0; i < attackCount; i++) {
                int blocks = (i == 0) ? baseBlocks : currentHeight;

                Vec3 targetPos = new Vec3(mc.player.getX(), mc.player.getY() + blocks, mc.player.getZ());
                sendMove(targetPos);
                sendMove(previousPos);
                mc.player.setPos(previousPos);

                sendingAttacks = true;
                if (swing.get() && mc.player != null) {
                    nl.oxod.nekoclient.utils.player.SwingMode.Client.attackSwing(InteractionHand.MAIN_HAND);
                }
                mc.getConnection().send(new ServerboundAttackPacket(living.getId()));

                currentHeight += increase.get();
            }

            positionCache.clear();

            if (useOffset.get()) {
                Vec3 offsetHome = getOffset(previousPos);
                sendMove(offsetHome);
                mc.player.setPos(offsetHome);
            }
        } finally {
            sendingAttacks = false;
        }
    }

    private void sendMove(Vec3 pos) {
        if (mc.getConnection() == null) return;
        ServerboundMovePlayerPacket movepacket = new ServerboundMovePlayerPacket.PosRot(pos.x, pos.y, pos.z, mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision);
        ((IServerboundMovePlayerPacket) movepacket).neko$setTag(1337);
        mc.getConnection().send(movepacket);
    }

    private Vec3 getOffset(Vec3 base) {
        double dx = offsetHorizontal.get();
        double dy = offsetY.get();

        Vec3[] shuffledOffsets = new Vec3[] {
            base.add(dx, dy, 0),
            base.add(-dx, dy, 0),
            base.add(0, dy, dx),
            base.add(0, dy, -dx),
            base.add(dx, dy, dx),
            base.add(-dx, dy, -dx),
            base.add(-dx, dy, dx),
            base.add(dx, dy, -dx)
        };

        List<Vec3> list = Arrays.asList(shuffledOffsets);
        Collections.shuffle(list);

        for (Vec3 pos : shuffledOffsets) {
            if (!invalid(pos)) return pos;
        }

        Vec3 noHorizontal = base.add(0, dy, 0);
        if (!invalid(noHorizontal)) return noHorizontal;

        return base;
    }

    private boolean invalid(Vec3 pos) {
        if (mc.level == null || mc.player == null) return true;

        BlockPos floored = BlockPos.containing(pos);
        int chunkX = floored.getX() >> 4;
        int chunkZ = floored.getZ() >> 4;
        if (mc.level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return true;

        if (positionCache.containsKey(pos)) return positionCache.get(pos);

        Vec3 delta = pos.subtract(mc.player.position());
        AABB box = mc.player.getBoundingBox().move(delta);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    mutablePos.set(floored.getX() + x, floored.getY() + y, floored.getZ() + z);
                    BlockState state = mc.level.getBlockState(mutablePos);
                    if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                        || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE)
                        || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)) {
                        positionCache.put(pos, true);
                        return true;
                    }
                }
            }
        }

        for (Entity e : mc.level.getEntities(mc.player, box)) {
            if (e.canBeCollidedWith(mc.player)) {
                positionCache.put(pos, true);
                return true;
            }
        }

        boolean collides = mc.level.getBlockCollisions(mc.player, box).iterator().hasNext();
        positionCache.put(pos, collides);
        return collides;
    }
}
