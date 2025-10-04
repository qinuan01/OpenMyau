package myau.module.modules;

import myau.Myau; // 导入主类以访问事件管理器
import myau.event.EventTarget; // 导入事件注解
import myau.event.types.EventType; // 导入事件类型
import myau.events.TickEvent; // 导入Tick事件类
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

/**
 * 最终版本的自动挖矿模块
 * 完美适配你的客户端框架
 */
public class AutoMiner extends Module {

    // 添加一个设置项，让用户可以在GUI中开关“基岩转向”功能
    public final BooleanProperty turnFromBedrock = new BooleanProperty("Turn From Bedrock", true);

    public AutoMiner() {
        super("AutoMiner", false);
    }

    /**
     * 当模块通过按键或指令被启用时，此方法会被调用。
     * 我们在这里将模块注册到事件总线，这样 onTick 方法才能开始接收事件。
     */
    @Override
    public void onEnabled() {
        super.onEnabled();
        // 假设你的事件管理器可以通过 Myau.eventManager 访问
        Myau.eventManager.register(this);
    }

    /**
     * 当模块被禁用时，此方法会被调用。
     * 在这里，我们必须做两件重要的事情：
     * 1. 停止玩家移动。
     * 2. 将模块从事件总线中注销，以停止接收 onTick 事件，节省性能。
     */
    @Override
    public void onDisabled() {
        super.onDisabled();
        // 1. 从事件总线注销
        Myau.eventManager.unregister(this);

        // 2. 确保玩家停止前进
        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        }
    }

    /**
     * 这是一个事件监听方法。
     * 只有当模块被启用并注册到事件总线后，这个方法才会被调用。
     * 它会接收 ModuleManager 广播的 TickEvent 事件。
     * @param event Tick事件对象
     */
    @EventTarget
    public void onTick(TickEvent event) {
        // 确保只在游戏的 PRE 阶段执行逻辑，与你的 ModuleManager 保持一致
        if (event.getType() != EventType.PRE) {
            return;
        }

        // 安全检查
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        // 如果打开了任何GUI界面，则暂停所有活动
        if (mc.currentScreen != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            return;
        }

        // 1. 自动前进
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);

        // 2. 检测准星对准的方块
        MovingObjectPosition objectMouseOver = mc.objectMouseOver;
        if (objectMouseOver == null || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return; // 没有对准方块，则只保持前进
        }

        BlockPos targetPos = objectMouseOver.getBlockPos();
        
        // 3. 判断是否为基岩
        if (mc.theWorld.getBlockState(targetPos).getBlock() == Blocks.bedrock) {
            if (turnFromBedrock.getValue()) {
                turnAway();
            }
            return; // 遇到基岩，停止挖掘
        }

        // 4. 挖掘方块
        mc.playerController.onPlayerDamageBlock(targetPos, objectMouseOver.sideHit);
        mc.thePlayer.swingItem();
    }

    /**
     * 转身的辅助方法
     */
    private void turnAway() {
        EnumFacing facing = mc.thePlayer.getHorizontalFacing();
        mc.thePlayer.setRotationYaw(facing.rotateY().getHorizontalAngle());
    }
}
