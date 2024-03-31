package com.direwolf20.justdirethings.client.screens;

import com.direwolf20.justdirethings.client.screens.basescreens.BaseMachineScreen;
import com.direwolf20.justdirethings.client.screens.standardbuttons.ToggleButtonFactory;
import com.direwolf20.justdirethings.client.screens.widgets.ToggleButton;
import com.direwolf20.justdirethings.common.containers.ClickerT1Container;
import com.direwolf20.justdirethings.common.network.data.DirectionSettingPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClickerT1Screen extends BaseMachineScreen<ClickerT1Container> {
    public ClickerT1Screen(ClickerT1Container container, Inventory inv, Component name) {
        super(container, inv, name);
    }

    @Override
    public void init() {
        super.init();
        addRenderableWidget(ToggleButtonFactory.DIRECTIONBUTTON(getGuiLeft() + 122, topSectionTop + 38, direction, b -> {
            ((ToggleButton) b).nextTexturePosition();
            direction = ((ToggleButton) b).getTexturePosition();
            PacketDistributor.SERVER.noArg().send(new DirectionSettingPayload(direction));
        }));
    }

    @Override
    public void setTopSection() {
        extraWidth = 0;
        extraHeight = 0;
    }

    @Override
    public void addRedstoneButtons() {
        addRenderableWidget(ToggleButtonFactory.REDSTONEBUTTON(getGuiLeft() + 104, topSectionTop + 38, redstoneMode.ordinal(), b -> {
            redstoneMode = redstoneMode.next();
            ((ToggleButton) b).nextTexturePosition();
            saveSettings();
        }));
    }
}
