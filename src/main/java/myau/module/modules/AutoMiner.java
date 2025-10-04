package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft; // 新增：导入 Minecraft 类
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class AutoMiner extends Module {

    // 新增：获取 Minecraft 实例，这是解决所有 "cannot find symbol: mc" 错误的关键
    private final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty turnFromBedrock = new BooleanProperty("Turn From Bedrock", true);

    public AutoMiner() {
        super("AutoMiner", false);
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        // 修正：通过 Myau.INSTANCE 访问 eventManager
        Myau.INSTANCE.eventManager.register(this);
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        // 修正：通过 Myau.INSTANCE 访问 eventManager
        Myau.INSTANCE.eventManager.unregister(this);

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
        if (objectMouseOver == null || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        BlockPos targetPos = objectMouseOver.getBlockPos();
        
        if (mc.theWorld.getBlockState(targetPos).getBlock() == Blocks.bedrock) {
            if (turnFromBedrock.getValue()) {
                turnAway();
            }
            return;
        }

        mc.playerController.onPlayerDamageBlock(targetPos, objectMouseOver.sideHit);
        mc.thePlayer.swingItem();
    }

    private void turnAway() {
        EnumFacing facing = mc.thePlayer.getHorizontalFacing();
        float targetYaw = mc.thePlayer.rotationYaw;

        // 修正：使用更可靠的 switch 语句进行转向，避免方法找不到的错误
        switch (facing) {
            case NORTH: // 当前朝向北 (-180 或 180)，向右转到东 (-90)
                targetYaw = -90.0f;
                break;
            case EAST:  // 当前朝向东 (-90)，向右转到南 (0)
                targetYaw = 0.0f;
                break;
            case SOUTH: // 当前朝向南 (0)，向右转到西 (90)
                targetYaw = 90.0f;
                break;
            case WEST:  // 当前朝向西 (90)，向右转到北 (180)
                targetYaw = 180.0f;
                break;
        }
        mc.thePlayer.setRotationYaw(targetYaw);
    }
}
