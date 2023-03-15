package fuzs.resourcepackoverrides.client.handler;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import fuzs.resourcepackoverrides.client.data.ResourceOverridesManager;
import fuzs.resourcepackoverrides.client.gui.screens.packs.PackAwareSelectionEntry;
import fuzs.resourcepackoverrides.mixin.client.accessor.AbstractSelectionListAccessor;
import fuzs.resourcepackoverrides.mixin.client.accessor.PackEntryAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PackActionsHandler {
    private static final Int2ObjectMap<PackAction> PACK_ACTIONS = new Int2ObjectOpenHashMap<>();
    private static boolean debugTooltips;

    static {
        PACK_ACTIONS.put(InputConstants.KEY_C, new PackAction(new TranslatableComponent("packAction.copyId.title"), new TranslatableComponent("packAction.copyId.description", new TextComponent("C").withStyle(ChatFormatting.BOLD)), new TranslatableComponent("packAction.copyId.success")) {

            @Override
            boolean execute(Minecraft minecraft) {
                Optional<String> hoveredPackId = getHoveredPackId(minecraft.screen);
                hoveredPackId.ifPresent(minecraft.keyboardHandler::setClipboard);
                return hoveredPackId.isPresent();
            }
        });
        PACK_ACTIONS.put(InputConstants.KEY_D, new PackAction(new TranslatableComponent("packAction.toggleDebug.title"), new TranslatableComponent("packAction.toggleDebug.description", new TextComponent("D").withStyle(ChatFormatting.BOLD)), new TranslatableComponent("packAction.toggleDebug.success")) {

            @Override
            boolean execute(Minecraft minecraft) {
                debugTooltips = !debugTooltips;
                return true;
            }
        });
        PACK_ACTIONS.put(InputConstants.KEY_R, new PackAction(new TranslatableComponent("packAction.reloadSettings.title"), new TranslatableComponent("packAction.reloadSettings.description", new TextComponent("R").withStyle(ChatFormatting.BOLD)), new TranslatableComponent("packAction.reloadSettings.success")) {

            @Override
            boolean execute(Minecraft minecraft) {
                ResourceOverridesManager.load();
                if (minecraft.screen != null) {
                    minecraft.screen.init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
                }
                return true;
            }
        });
    }

    public static void onScreen$Render$Post(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (debugTooltips) {
            getHoveredPackId(screen).map(TextComponent::new).ifPresent(component -> {
                screen.renderTooltip(poseStack, component, mouseX, mouseY);
            });
        }
    }

    private static Optional<String> getHoveredPackId(@Nullable Screen screen) {
        if (screen == null) return Optional.empty();
        for (GuiEventListener guiEventListener : screen.children()) {
            if (guiEventListener instanceof ObjectSelectionList<?> selectionList) {
                if (((AbstractSelectionListAccessor<?>) selectionList).resourcepackoverrides$callGetHovered() instanceof TransferableSelectionList.PackEntry entry) {
                    if (((PackEntryAccessor) entry).resourcepackoverrides$getPack() instanceof PackAwareSelectionEntry selectionEntry) {
                        return Optional.of(selectionEntry.getPackId());
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static void onClientTick$End(Minecraft minecraft) {
        PACK_ACTIONS.values().forEach(action -> action.tick(minecraft));
    }

    public static void onKeyPressed$Post(Screen screen, int keyCode, int scanCode, int modifiers) {
        PackAction packAction = PACK_ACTIONS.get(keyCode);
        if (packAction != null) packAction.update();
    }

    private static abstract class PackAction {
        private final Component title;
        private final Component description;
        private final Component success;
        @Nullable
        private TutorialToast toast;
        @Nullable
        private TutorialToast successToast;
        private int successTicks;
        private int pressTime;
        private int lastPressTime;
        private int decreaseTimeDelay;
        private boolean wasExecuted;

        public PackAction(Component title, Component description, Component success) {
            this.title = title;
            this.description = description;
            this.success = success;
        }

        public void tick(Minecraft minecraft) {
            if (this.pressTime == this.lastPressTime && this.pressTime > 0) {
                if (--this.decreaseTimeDelay < 0) {
                    if (this.wasExecuted) {
                        this.reset();
                    } else {
                        this.pressTime--;
                    }
                }
            }
            this.lastPressTime = this.pressTime;
            if (this.pressTime > 0) {
                if (this.toast == null) {
                    this.toast = new TutorialToast(TutorialToast.Icons.MOVEMENT_KEYS, this.title, this.description, true);
                    minecraft.getToasts().addToast(this.toast);
                }
                if (this.pressTime < 20) {
                    this.toast.updateProgress(Mth.clamp(this.pressTime / 20.0F, 0.0F, 1.0F));
                } else if (!this.wasExecuted) {
                    if (this.execute(minecraft)) {
                        this.finish(minecraft);
                    }
                    this.wasExecuted = true;
                    this.toast.updateProgress(1.0F);
                }
            } else {
                this.reset();
            }
            if (this.successTicks > 0) {
                this.successTicks--;
                this.successToast.updateProgress(this.successTicks / 80.0F);
            } else if (this.successToast != null) {
                this.successToast.hide();
                this.successToast = null;
            }
        }

        private void reset() {
            if (this.toast != null) {
                this.toast.hide();
                this.toast = null;
            }
            this.pressTime = this.lastPressTime = 0;
            this.wasExecuted = false;
        }

        abstract boolean execute(Minecraft minecraft);

        private void finish(Minecraft minecraft) {
            if (this.successToast != null) this.successToast.hide();
            this.successToast = new TutorialToast(TutorialToast.Icons.MOVEMENT_KEYS, this.title, this.success, true);
            minecraft.getToasts().addToast(this.successToast);
            this.successTicks = 80;
            this.successToast.updateProgress(1.0F);
        }

        public void update() {
            this.pressTime++;
            this.resetDelay();
        }

        public void resetDelay() {
            // this high of a delay is necessary as for some reason the key takes a few ticks until it reports as pressed again after the initial press
            this.decreaseTimeDelay = 10;
        }
    }
}
