package io.github.astelev.around_planet.client;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import io.github.astelev.around_planet.space.SpaceCells;
@EventBusSubscriber(modid="around_planet",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class OrbitMapKeys {
    public static final KeyMapping MAP=new KeyMapping("key.around_planet.orbit_map",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_M,"key.categories.around_planet");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){event.register(MAP);}
    /** Consumed before ordinary KeyMapping/InputEvent dispatch, so shared M does not open a second map. */
    public static boolean handle(long window,int key,int scan,int action){
        var mc=Minecraft.getInstance();if(window!=mc.getWindow().getWindow() || !MAP.matches(key,scan))return false;
        if(mc.screen instanceof OrbitMapScreen){if(action==GLFW.GLFW_PRESS)mc.screen.onClose();return true;}
        if(mc.screen!=null || mc.player==null || !SpaceCells.contains(mc.player))return false;
        if(action==GLFW.GLFW_PRESS)mc.setScreen(new OrbitMapScreen());return true;
    }
    private OrbitMapKeys(){}
}
