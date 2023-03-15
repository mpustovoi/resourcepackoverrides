package fuzs.resourcepackoverrides.client;

import fuzs.resourcepackoverrides.ResourcePackOverrides;
import fuzs.resourcepackoverrides.client.handler.PackActionsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@Mod.EventBusSubscriber(modid = ResourcePackOverrides.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ResourcePackOverridesForgeClient {

    @SubscribeEvent
    public static void onConstructMod(final FMLConstructModEvent evt) {
        registerHandlers();
    }

    private static void registerHandlers() {
        MinecraftForge.EVENT_BUS.addListener((final ScreenEvent.DrawScreenEvent.Post evt) -> {
            if (evt.getScreen() instanceof PackSelectionScreen) {
                PackActionsHandler.onScreen$Render$Post(evt.getScreen(), evt.getPoseStack(), evt.getMouseX(), evt.getMouseY(), evt.getPartialTicks());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((final ScreenEvent.KeyboardKeyPressedEvent.Post evt) -> {
            if (evt.getScreen() instanceof PackSelectionScreen) {
                PackActionsHandler.onKeyPressed$Post(evt.getScreen(), evt.getKeyCode(), evt.getScanCode(), evt.getModifiers());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((final TickEvent.ClientTickEvent evt) -> {
            if (evt.phase == TickEvent.Phase.END) PackActionsHandler.onClientTick$End(Minecraft.getInstance());
        });
    }
}
