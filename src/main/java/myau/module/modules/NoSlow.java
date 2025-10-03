package myau.module.modules;

import myau.Myau;
import myau.enums.FloatModules;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.LivingUpdateEvent;
import myau.events.PlayerUpdateEvent;
import myau.events.RightClickMouseEvent;
import myau.module.Module;
import myau.util.BlockUtil;
import myau.util.ItemUtil;
import myau.util.PlayerUtil;
import myau.util.TeamUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.ModeProperty;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int lastSlot = -1;

    // sword-mode 增加 HYPIXEL
    public final ModeProperty swordMode = new ModeProperty("sword-mode", 1, new String[]{"NONE", "VANILLA", "HYPIXEL"});
    public final PercentProperty swordMotion = new PercentProperty("sword-motion", 100, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty swordSprint = new BooleanProperty("sword-sprint", true, () -> this.swordMode.getValue() != 0);

    public final ModeProperty foodMode = new ModeProperty("food-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
    public final PercentProperty foodMotion = new PercentProperty("food-motion", 100, () -> this.foodMode.getValue() != 0);
    public final BooleanProperty foodSprint = new BooleanProperty("food-sprint", true, () -> this.foodMode.getValue() != 0);

    public final ModeProperty bowMode = new ModeProperty("bow-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT"});
    public final PercentProperty bowMotion = new PercentProperty("bow-motion", 100, () -> this.bowMode.getValue() != 0);
    public final BooleanProperty bowSprint = new BooleanProperty("bow-sprint", true, () -> this.bowMode.getValue() != 0);

    // === HypixelNoSlow 相关 ===
    private boolean hypixelCycle = false;
    private final NormalBlink hypixelBlink = new NormalBlink();

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isFloatMode() {
        return this.foodMode.getValue() == 2 && ItemUtil.isEating()
                || this.bowMode.getValue() == 2 && ItemUtil.isUsingBow();
    }

    public boolean isAnyActive() {
        return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return this.isSwordActive() && this.swordSprint.getValue()
                || this.isFoodActive() && this.foodSprint.getValue()
                || this.isBowActive() && this.bowSprint.getValue();
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) {
            return this.swordMotion.getValue();
        } else if (ItemUtil.isEating()) {
            return this.foodMotion.getValue();
        } else {
            return ItemUtil.isUsingBow() ? this.bowMotion.getValue() : 100;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.isAnyActive()) {
            float multiplier = (float) this.getMotionMultiplier() / 100.0F;
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;
            if (!this.canSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!this.isEnabled()) return;

        // ======= Hypixel 剑模式逻辑 =======
        if (this.swordMode.getValue() == 2 && ItemUtil.isHoldingSword() && mc.thePlayer.isUsingItem()) {
            if (hypixelCycle) {
                if (!hypixelBlink.isEnabled()) hypixelBlink.enable();

                mc.getNetHandler().addToSendQueue(
                        new C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                                BlockPos.ORIGIN,
                                EnumFacing.DOWN
                        )
                );
                hypixelCycle = false;
            } else {
                if (hypixelBlink.isEnabled()) hypixelBlink.disable();

                mc.getNetHandler().addToSendQueue(
                        new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem())
                );
                hypixelCycle = true;
            }
        } else {
            hypixelCycle = false;
            if (hypixelBlink.isEnabled()) hypixelBlink.disable();
        }

        // ======= Float Mode 原逻辑 =======
        if (this.isFloatMode()) {
            int item = mc.thePlayer.inventory.currentItem;
            if (this.lastSlot != item && PlayerUtil.isUsingItem()) {
                this.lastSlot = item;
                Myau.floatManager.setFloatState(true, FloatModules.NO_SLOW);
            }
        } else {
            this.lastSlot = -1;
            Myau.floatManager.setFloatState(false, FloatModules.NO_SLOW);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.objectMouseOver != null) {
                switch (mc.objectMouseOver.typeOfHit) {
                    case BLOCK:
                        BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                        if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) {
                            return;
                        }
                        break;
                    case ENTITY:
                        Entity entityHit = mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) {
                            return;
                        }
                        if (entityHit instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase) entityHit)) {
                            return;
                        }
                }
            }
            if (this.isFloatMode() && !Myau.floatManager.isPredicted() && mc.thePlayer.onGround) {
                event.setCancelled(true);
                mc.thePlayer.motionY = 0.42F;
            }
        }
    }

    // ===================================================
    // === 内嵌 Blink 类（简化 NormalBlink）===============
    // ===================================================
    private static class NormalBlink {
        private boolean enabled = false;
        private final java.util.List<net.minecraft.network.Packet<?>> blinkedPackets = new java.util.ArrayList<>();

        public void enable() {
            if (!enabled) {
                blinkedPackets.clear();
                enabled = true;
            }
        }

        public void disable() {
            if (enabled) {
                flush();
                enabled = false;
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        private void flush() {
            synchronized (blinkedPackets) {
                for (net.minecraft.network.Packet<?> p : blinkedPackets) {
                    mc.getNetHandler().addToSendQueue(p);
                }
            }
            blinkedPackets.clear();
        }

        // Hook 供外部调用，如果你有事件系统可以在 onSendPacket 调用
        public boolean onSendPacket(net.minecraft.network.Packet<?> packet) {
            if (!enabled) return false;
            blinkedPackets.add(packet);
            return true; // 表示 cancel 掉原本的发送
        }
    }
}
