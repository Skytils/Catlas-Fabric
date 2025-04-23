/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2023 Skytils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package gg.skytils.skytilsmod.mixins.transformers.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.skytils.skytilsmod.features.impl.handlers.ChatTabs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.class_0_686;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Mixin(value = ChatHud.class, priority = 1001)
public abstract class MixinGuiNewChat extends DrawContext {


    private static final ChatHudLine skytils$placeholderLine = new ChatHudLine(0, new LiteralTextContent("skytils placeholder"), 0);

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract int getVisibleLineCount();

    @WrapOperation(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/ChatMessages;breakRenderedChatMessageLines(Lnet/minecraft/text/Text;ILnet/minecraft/client/TextRenderer;ZZ)Ljava/util/List;"))
    private List<Text> filterDrawnTextComponents(Text p_178908_0_, int p_178908_1_, TextRenderer p_178908_2_, boolean p_178908_3_, boolean p_178908_4_, Operation<List<Text>> original) {
        return ChatTabs.INSTANCE.shouldAllow(p_178908_0_) ? original.call(p_178908_0_, p_178908_1_, p_178908_2_, p_178908_3_, p_178908_4_) : Collections.emptyList();
    }

    @Redirect(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;getString()Ljava/lang/String;"))
    private String printFormattedText(Text iChatComponent) {
        return iChatComponent.method_10865();
    }

    @Inject(method = "getTextStyleAt", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/ChatHud;scrolledLines:I"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void stopOutsideWindow(int mouseX, int mouseY, CallbackInfoReturnable<Text> cir, class_0_686 scaledresolution, int i, float f, int j, int k, int l) {
        int line = k / this.client.textRenderer.field_0_2811;
        if (line >= getVisibleLineCount()) cir.setReturnValue(null);
    }

    @Inject(method = "removeMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine;getId()I"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void stopDeleteCrash(int id, CallbackInfo ci, Iterator<ChatHudLine> iterator, ChatHudLine chatline) {
        if (chatline == null || skytils$placeholderLine == chatline) {
            iterator.remove();
        }
    }

    @ModifyVariable(method = "removeMessage", at = @At("STORE"))
    private ChatHudLine stopDeleteCrash(ChatHudLine chatLine) {
        return chatLine == null ? skytils$placeholderLine : chatLine;
    }
}
