package dev.walkercompat.mixin;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.letitwalk.client.PilotControlsScreen;
import dev.letitwalk.content.PilotControlsMenu;
import dev.walkercompat.link.PagedSlot;
import dev.walkercompat.link.ReceiveAction;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds page arrows to the Pilot Controls screen: page 0 = Send keys, pages 1+ = Receive actions. */
@Mixin(value = PilotControlsScreen.class, remap = false)
public abstract class PilotControlsScreenMixin extends AbstractSimiContainerScreen<PilotControlsMenu> {
    @Unique
    private static final int WALKERCOMPAT_PAGES = ReceiveAction.PAGES + 1;

    protected PilotControlsScreenMixin(PilotControlsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void walkercompat$startOnSendPage(PilotControlsMenu menu, Inventory inv, Component title, CallbackInfo ci) {
        PagedSlot.shownPage = 0;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void walkercompat$addPageButtons(CallbackInfo ci) {
        int x = this.leftPos;
        int y = this.topPos;
        IconButton prev = new IconButton(x + 10, y + 109 - 24, AllIcons.I_MTD_LEFT);
        prev.withCallback(() -> PagedSlot.shownPage = (PagedSlot.shownPage + WALKERCOMPAT_PAGES - 1) % WALKERCOMPAT_PAGES);
        IconButton next = new IconButton(x + 88, y + 109 - 24, AllIcons.I_MTD_RIGHT);
        next.withCallback(() -> PagedSlot.shownPage = (PagedSlot.shownPage + 1) % WALKERCOMPAT_PAGES);
        this.addRenderableWidget(prev);
        this.addRenderableWidget(next);
    }

    /** Column labels: the redstone keys on page 0, our actions on the receive pages. */
    @Redirect(method = "renderBg", at = @At(value = "INVOKE",
            target = "Ldev/letitwalk/client/PilotControlsScreen;keyName(I)Lnet/minecraft/network/chat/Component;"))
    private Component walkercompat$columnLabel(int column) {
        if (PagedSlot.shownPage == 0) {
            return PilotControlsScreen.keyName(column);
        }
        ReceiveAction action = ReceiveAction.at(PagedSlot.shownPage, column);
        return action != null ? Component.translatable(action.shortKey()) : Component.empty();
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void walkercompat$drawPageInfo(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        int x = this.leftPos;
        int y = this.topPos;
        int page = PagedSlot.shownPage;
        // grey out empty columns on the last receive page
        if (page > 0) {
            for (int column = 0; column < ReceiveAction.PER_PAGE; column++) {
                if (ReceiveAction.at(page, column) == null) {
                    graphics.fill(x + 11 + 24 * column, y + 33, x + 29 + 24 * column, y + 69, 0xC0202630);
                }
            }
        }
        Component label = Component.translatable("walker_controller_compat.page." + page);
        Component count = Component.literal((page + 1) + "/" + WALKERCOMPAT_PAGES);
        int centre = x + 10 + 18 + (88 - 10 - 18) / 2;
        graphics.drawString(this.font, label, centre - this.font.width(label) / 2, y + 109 - 23, 0x3F3F3F, false);
        graphics.drawString(this.font, count, centre - this.font.width(count) / 2, y + 109 - 13, 0x6F6F6F, false);
    }

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void walkercompat$receiveTooltip(List<Component> list, int slot, CallbackInfoReturnable<List<Component>> cir) {
        int r = slot - ReceiveAction.SEND_SLOTS;
        if (r >= 0 && r < ReceiveAction.RECEIVE_SLOTS) {
            ReceiveAction action = ReceiveAction.ALL[r / 2];
            String name = Component.translatable(action.nameKey()).getString();
            list.add(CreateLang.translateDirect("linked_controller.frequency_slot_" + (r % 2 + 1), name).withStyle(ChatFormatting.GOLD));
            list.add(Component.translatable("walker_controller_compat.receive_hint").withStyle(ChatFormatting.GRAY));
            cir.setReturnValue(list);
        }
    }
}
