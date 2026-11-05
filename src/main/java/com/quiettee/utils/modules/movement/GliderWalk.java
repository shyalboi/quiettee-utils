package com.quiettee.utils.modules.movement;

import com.quiettee.utils.QuietteeUtils;
import com.quiettee.utils.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.LocalPlayerAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class GliderWalk extends Module {
    private static final int TRACKED_FLAGS_ID = 0;
    private static final int GLIDING_BIT = 0x80;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final SettingGroup sgShow = settings.createGroup("Show");

    private final Setting<Boolean> groundFly = sgGeneral.add(new BoolSetting.Builder()
        .name("ground-fly")
        .description("Skate at elytra speed on the ground.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> flySpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fly-speed")
        .description("Ground fly speed in blocks per tick.")
        .defaultValue(1.5)
        .min(0.0)
        .sliderRange(0.0, 3.0)
        .visible(groundFly::get)
        .build()
    );

    private final Setting<Boolean> hideNametag = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-nametag")
        .description("Hide your nametag.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-elytra")
        .description("Swap to an elytra automatically.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minDurability = sgGeneral.add(new IntSetting.Builder()
        .name("min-durability")
        .description("Drop the pose below this durability.")
        .defaultValue(20)
        .min(0)
        .sliderRange(0, 200)
        .build()
    );

    private final Setting<Boolean> showSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("show-self")
        .description("Show the pose on your own client.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> retryRate = sgGeneral.add(new IntSetting.Builder()
        .name("retry-rate")
        .description("Minimum ticks between pose retries.")
        .defaultValue(4)
        .min(1)
        .sliderRange(1, 60)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only claim while standing on something.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> holdStill = sgGeneral.add(new BoolSetting.Builder()
        .name("hold-still")
        .description("Keep the pose while standing still.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> spin = sgShow.add(new BoolSetting.Builder()
        .name("spin")
        .description("Spin your yaw for onlookers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> spinSpeed = sgShow.add(new DoubleSetting.Builder()
        .name("spin-speed")
        .description("Degrees of yaw per tick.")
        .defaultValue(18.0)
        .min(-90.0)
        .sliderRange(-90.0, 90.0)
        .visible(spin::get)
        .build()
    );

    private final Setting<Double> pitchLock = sgShow.add(new DoubleSetting.Builder()
        .name("pitch-lock")
        .description("Report a fixed pitch.")
        .defaultValue(-181.0)
        .min(-181.0)
        .sliderRange(-181.0, 90.0)
        .build()
    );

    private final Setting<Boolean> bob = sgShow.add(new BoolSetting.Builder()
        .name("bob")
        .description("Bob your pitch on a sine wave.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> bobRange = sgShow.add(new DoubleSetting.Builder()
        .name("bob-range")
        .description("Bob degrees either side of level.")
        .defaultValue(35.0)
        .min(0.0)
        .sliderRange(0.0, 90.0)
        .visible(bob::get)
        .build()
    );

    private final Setting<Double> bobSpeed = sgShow.add(new DoubleSetting.Builder()
        .name("bob-speed")
        .description("Bob speed in radians per tick.")
        .defaultValue(0.1)
        .min(0.01)
        .sliderRange(0.01, 0.5)
        .visible(bob::get)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Chat when the pose lights or fails.")
        .defaultValue(true)
        .build()
    );

    private int timer;
    private int chestSlot = -1;
    private boolean rewritingInput;
    private boolean rewritingMove;
    private float spinYaw;
    private int showTicks;
    private boolean serverGliding;
    private boolean announced;
    private int attempts;

    private int pending;
    private boolean airRelight;
    private boolean sawMove;
    private int airRelights;

    public GliderWalk() {
        super(QuietteeUtils.CATEGORY, "glider-walk", "Walk on the ground in the elytra flight pose.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        serverGliding = mc.player != null && mc.player.isFallFlying();
        announced = false;
        attempts = 0;
        pending = 0;
        airRelight = false;
        sawMove = false;
        airRelights = 0;

        chestSlot = -1;
        spinYaw = mc.player == null ? 0.0F : mc.player.getYRot();
        showTicks = 0;
        if (autoElytra.get() && !hasElytra()) equipElytra();

        if (!hasElytra()) warning("no elytra equipped - canGlide() checks your chest slot, so the server will refuse the pose.");
        else if (mc.player != null && !mc.player.onGround()) info("you are in the air - land first. This is a walking trick; it does nothing while you are really flying.");
    }

    @Override
    public void onDeactivate() {
        if (mc.player == null || mc.getConnection() == null) return;

        boolean clientFlying = mc.player.isFallFlying() && !mc.player.onGround() && !mc.player.isInWater();

        if (serverGliding && !clientFlying) mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        if (mc.player.isFallFlying() && !clientFlying) mc.player.stopFallFlying();
        serverGliding = clientFlying && serverGliding;
        pending = 0;
        airRelight = false;

        if (spin.get() || bob.get() || pitchLock.get() > -180.5) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));
        }

        if (hideNametag.get()) sendInput(false);
        if (autoElytra.get() && chestSlot != -1) restoreChest();
    }

    private int echoWait() {
        int rtt = (int) Math.ceil(PlayerUtils.getPing() / 50.0) + 3;
        return Math.min(60, Math.max(retryRate.get(), rtt));
    }

    private boolean hasElytra() {
        return mc.player != null && mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER);
    }

    private boolean claiming() {
        if (mc.player == null || mc.getConnection() == null) return false;
        if (!hasElytra()) return false;
        if (mc.player.isInWater()) return false;
        if (mc.player.isPassenger()) return false;
        return true;
    }

    private boolean engaged() {
        return claiming() && (!onlyOnGround.get() || mc.player.onGround());
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getConnection() == null) return;

        sawMove = false;
        airRelight = false;
        if (pending > 0) pending--;

        if (!claiming()) return;
        if (holdStill.get()) ((LocalPlayerAccessor) mc.player).meteor$setPositionReminder(20);
        if (!engaged()) {

            if (showSelf.get() && mc.player.isFallFlying() && !mc.player.onGround() && !serverGliding && pending == 0) {
                airRelight = true;
                ((LocalPlayerAccessor) mc.player).meteor$setPositionReminder(20);
            }
            return;
        }

        if (!serverGliding && pending == 0 && --timer <= 0) {
            timer = retryRate.get();
            light();
        }

        if (minDurability.get() > 0) {
            ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.isDamageableItem() && chest.getMaxDamage() - chest.getDamageValue() <= minDurability.get()) {
                warning("elytra down to %d durability - dropping the pose before it breaks.", chest.getMaxDamage() - chest.getDamageValue());
                toggle();
                return;
            }
        }

        if (holdStill.get()) ((LocalPlayerAccessor) mc.player).meteor$setPositionReminder(20);

        showTicks++;
        if (spin.get()) spinYaw = Mth.wrapDegrees(spinYaw + spinSpeed.get().floatValue());

        if (hideNametag.get() && timer % 10 == 0) sendInput(true);

        if (!showSelf.get() && mc.player.isFallFlying()) mc.player.stopFallFlying();
    }

    private void light() {
        mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision));

        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));

        if (++attempts == 8 && !serverGliding && notify.get()) {
            warning("the server keeps refusing the pose - check you have an elytra on, are not in water or a vehicle, and that NoFall's ground spoof is off (it claims the opposite flag).");
        }
    }

    @EventHandler
    private void onReceive(PacketEvent.Receive event) {
        if (mc.player == null || !(event.packet instanceof ClientboundSetEntityDataPacket p) || p.id() != mc.player.getId()) return;

        for (SynchedEntityData.DataValue<?> entry : p.packedItems()) {
            if (entry.id() == TRACKED_FLAGS_ID && entry.value() instanceof Byte b) {
                boolean now = (b & GLIDING_BIT) != 0;
                if (now && !serverGliding && !announced && notify.get()) {
                    announced = true;
                    info("pose lit - everyone can see you soaring.");
                }

                if (now || now != serverGliding) pending = 0;
                serverGliding = now;
            }
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (!airRelight || mc.player == null || mc.getConnection() == null) return;
        airRelight = false;
        if (!sawMove) return;

        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        airRelights++;
        if (notify.get() && airRelights <= 3) info("the server dropped your wings mid-air - re-opened them (#%d).", airRelights);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerMove(PlayerMoveEvent event) {
        if (!groundFly.get() || event.type != MoverType.SELF || !engaged()) return;
        if (mc.gui.screen() != null) return;

        Vec3 fwd = Vec3.directionFromRotation(0, mc.player.getYRot());
        Vec3 rgt = Vec3.directionFromRotation(0, mc.player.getYRot() + 90);

        double fx = 0, fz = 0;
        if (mc.options.keyUp.isDown()) { fx += fwd.x; fz += fwd.z; }
        if (mc.options.keyDown.isDown())    { fx -= fwd.x; fz -= fwd.z; }
        if (mc.options.keyRight.isDown())   { fx += rgt.x; fz += rgt.z; }
        if (mc.options.keyLeft.isDown())    { fx -= rgt.x; fz -= rgt.z; }

        double len = Math.sqrt(fx * fx + fz * fz);
        if (len < 1e-6) return;

        double speed = flySpeed.get();
        ((IVec3) event.movement).meteor$set(fx / len * speed, event.movement.y, fz / len * speed);
    }

    private void sendInput(boolean sneak) {
        if (mc.player == null || mc.getConnection() == null) return;

        rewritingInput = true;
        try {
            mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(
                mc.options.keyUp.isDown(), mc.options.keyDown.isDown(),
                mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(),
                mc.options.keyJump.isDown(), sneak, mc.options.keySprint.isDown())));
        }
        finally {
            rewritingInput = false;
        }
    }

    private void equipElytra() {
        for (int i = 0; i < mc.player.getInventory().getNonEquipmentItems().size(); i++) {
            if (mc.player.getInventory().getNonEquipmentItems().get(i).has(DataComponents.GLIDER)) {
                chestSlot = i;
                InvUtils.move().from(i).toArmor(2);
                mc.getConnection().send(new ServerboundContainerClosePacket(0));
                return;
            }
        }
        warning("no elytra in the inventory to swap to.");
    }

    private void restoreChest() {
        if (chestSlot < 0 || chestSlot >= mc.player.getInventory().getNonEquipmentItems().size()) return;
        ItemStack back = mc.player.getInventory().getNonEquipmentItems().get(chestSlot);
        if (back.isEmpty() || back.has(DataComponents.GLIDER)) return;

        InvUtils.move().from(chestSlot).toArmor(2);
        mc.getConnection().send(new ServerboundContainerClosePacket(0));
        chestSlot = -1;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onSend(PacketEvent.Send event) {

        if (event.packet instanceof ServerboundPlayerCommandPacket c && c.getAction() == ServerboundPlayerCommandPacket.Action.START_FALL_FLYING) {
            pending = echoWait();
            return;
        }

        if (hideNametag.get() && !rewritingInput && event.packet instanceof ServerboundPlayerInputPacket in && claiming()) {
            Input i = in.input();
            if (!i.shift()) {
                event.cancel();
                sendInput(true);
            }
            return;
        }

        if (!(event.packet instanceof ServerboundMovePlayerPacket p) || !claiming()) return;
        ((PlayerMoveC2SPacketAccessor) p).quiettee$setOnGround(false);
        if (p.hasPosition()) sawMove = true;

        boolean lockPitch = pitchLock.get() > -180.5;
        if ((spin.get() || bob.get() || lockPitch) && !rewritingMove) {
            float yaw = spin.get() ? spinYaw : mc.player.getYRot();
            float pitch;
            if (bob.get()) pitch = (float) (Math.sin(showTicks * bobSpeed.get()) * bobRange.get());
            else if (lockPitch) pitch = pitchLock.get().floatValue();
            else pitch = mc.player.getXRot();

            if (p.hasRotation()) {
                ((PlayerMoveC2SPacketAccessor) p).quiettee$setYaw(yaw);
                ((PlayerMoveC2SPacketAccessor) p).quiettee$setPitch(pitch);
            }
            else {
                event.cancel();
                rewritingMove = true;
                try {
                    ServerboundMovePlayerPacket full = new ServerboundMovePlayerPacket.PosRot(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        yaw, pitch, false, mc.player.horizontalCollision);
                    mc.getConnection().send(full);
                }
                finally {
                    rewritingMove = false;
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        if (mc.player == null) return null;
        if (!hasElytra()) return "no elytra";
        if (onlyOnGround.get() && !mc.player.onGround()) return "land first";
        return serverGliding ? "soaring" : "lighting";
    }
}
