package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class AutoMiner extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty turnFromBedrock = new BooleanProperty("Turn From Bedrock", true);

    public AutoMiner() {
        super("AutoMiner", false);
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        // 修正：请将下面的 "你的事件管理器变量名" 替换成你在 Myau.java 文件中找到的真实变量名
        Myau.你的事件管理器变量名.register(this);
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        // 修正：请将下面的 "你的事件管理器变量名" 替换成你在 Myau.java 文件中找到的真实变量名
        Myau.你的事件管理器变量名.unregister(this);

        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (mc.currentScreen != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            return;
        }

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);

        MovingObjectPosition objectMouseOver = mc.objectMouseOver;
        if (objectMouseOver == null || objectMouseOver.typeOfHit != Moving_Object_Position.Moving_ObjectType.BLOCK) {
            return;
        }

        BlockPos targetPos = objectMouseOver.getBlockPos();

        if (mc.the_World.getBlockState(targetPos).getBlock() == Blocks.bedrock) {
            if (turnFromBedrock.getValue()) {
                turnAway();
            }
            return;
        }

        mc.playerController.onPlayerDamageBlock(targetPos, objectMouseOver.sideHit);
        mc.thePlayer.swingItem();
    }

    private void turnAway() {
        EntityPlayerSP player = mc.thePlayer;
        EnumFacing facing = player.getHorizontalFacing();
        float targetYaw = player.rotationYaw;

        switch (facing) {
            case NORTH:
                targetYaw = -90.0f; // East
                break;
            case EAST:
                targetYaw = 0.0f;   // South
                break;
            case SOUTH:
                targetYaw = 90.0f;  // West
                break;
            case WEST:
                targetYaw = 180.0f; // North
                break;
        }
        
        // 修正：在1.8.9版本中，直接给 rotationYaw 属性赋值，而不是调用 setRotationYaw() 方法
        player.rotationYaw = targetYaw;
    }
}
