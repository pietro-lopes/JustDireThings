package com.direwolf20.justdirethings.common.blockentities.basebe;

import com.direwolf20.justdirethings.setup.Registration;
import com.direwolf20.justdirethings.util.MiscHelpers;
import com.direwolf20.justdirethings.util.UsefulFakePlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

public class BaseMachineBE extends BlockEntity {
    public int MACHINE_SLOTS = 0;
    public static final UUID defaultFakePlayerUUID = UUID.fromString("4191a6f5-37fe-45d9-8ba3-4549be778e54");
    public static final GameProfile defaultFakePlayerProfile = new GameProfile(defaultFakePlayerUUID, "[JustDiresFakePlayer]");
    public UUID placedByUUID;
    protected int direction = 0;
    protected int tickSpeed = 20;
    protected int operationTicks = -1;
    protected UsefulFakePlayer usefulFakePlayer;

    public BaseMachineBE(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public void tickClient() {
    }

    public void tickServer() {
        handleTicks();
        if (this instanceof RedstoneControlledBE redstoneControlledBE)
            redstoneControlledBE.evaluateRedstone();
    }

    public void handleTicks() {
        if (operationTicks <= 0)
            operationTicks = tickSpeed;
        operationTicks--;
    }

    public int getTickSpeed() {
        return tickSpeed;
    }

    public void setTickSpeed(int newTickSpeed) {
        this.tickSpeed = newTickSpeed;
        if (operationTicks > tickSpeed)
            operationTicks = tickSpeed;
        markDirtyClient();
    }

    public boolean canRun() {
        if (this instanceof RedstoneControlledBE redstoneControlledBE)
            return operationTicks == 0 || redstoneControlledBE.getRedstoneControlData().redstoneMode.equals(MiscHelpers.RedstoneMode.PULSE);
        return operationTicks == 0;
    }

    public int getDirection() {
        return direction;
    }

    public Direction getDirectionValue() {
        return Direction.values()[direction];
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    protected GameProfile getPlacedByProfile() {
        if (placedByUUID == null)
            return defaultFakePlayerProfile;
        return new GameProfile(placedByUUID, "[JustDiresFakePlayer]");
    }

    protected FakePlayer getFakePlayer(ServerLevel level) {
        return FakePlayerFactory.get(level, getPlacedByProfile());
    }

    protected FakePlayer getFakePlayer(ServerLevel level, UUID uuid) {
        GameProfile gameProfile = new GameProfile(uuid, "[JustDiresFakePlayer]");
        return FakePlayerFactory.get(level, gameProfile);
    }

    protected UsefulFakePlayer getUsefulFakePlayer(ServerLevel level) {
        if (usefulFakePlayer == null)
            usefulFakePlayer = UsefulFakePlayer.createPlayer(level, getPlacedByProfile());
        return usefulFakePlayer;
    }

    public void setFakePlayerData(ItemStack itemStack, FakePlayer fakePlayer, BlockPos blockPos, Direction direction) {
        fakePlayer.setPos(blockPos.below().relative(direction).getX() + 0.5, blockPos.below().relative(direction).getY(), blockPos.below().relative(direction).getZ() + 0.5);
        float xRot = direction == Direction.DOWN ? 90 : direction == Direction.UP ? -90 : 0;
        fakePlayer.setXRot(xRot);
        fakePlayer.setYRot(direction.toYRot());
        fakePlayer.setYHeadRot(direction.toYRot());
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
    }

    public void setPlacedBy(UUID placedBy) {
        this.placedByUUID = placedBy;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        // Vanilla uses the type parameter to indicate which type of tile entity (command block, skull, or beacon?) is receiving the packet, but it seems like Forge has overridden this behavior
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ItemStackHandler getMachineHandler() {
        return getData(Registration.MACHINE_HANDLER);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        if (this instanceof AreaAffectingBE areaAffectingBE)
            areaAffectingBE.getAreaAffectingData().area = null; //Clear this cache when a packet comes in, so it can redraw properly if the area was changed
    }

    public void markDirtyClient() {
        setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this instanceof FilterableBE filterableBE)
            filterableBE.getFilterData().filterCache.clear();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("tickspeed", tickSpeed);
        if (placedByUUID != null)
            tag.putUUID("placedBy", placedByUUID);
        tag.putInt("direction", direction);
        if (this instanceof AreaAffectingBE areaAffectingBE)
            areaAffectingBE.saveAreaSettings(tag);
        if (this instanceof FilterableBE filterableBE)
            filterableBE.saveFilterSettings(tag);
        if (this instanceof RedstoneControlledBE redstoneControlledBE)
            redstoneControlledBE.saveRedstoneSettings(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("direction"))
            direction = tag.getInt("direction");
        if (tag.contains("tickspeed"))
            tickSpeed = tag.getInt("tickspeed");
        if (tag.contains("placedBy"))
            placedByUUID = tag.getUUID("placedBy");
        if (this instanceof AreaAffectingBE areaAffectingBE)
            areaAffectingBE.loadAreaSettings(tag);
        if (this instanceof FilterableBE filterableBE)
            filterableBE.loadFilterSettings(tag);
        if (this instanceof RedstoneControlledBE redstoneControlledBE)
            redstoneControlledBE.loadRedstoneSettings(tag);
        super.load(tag);
    }
}
