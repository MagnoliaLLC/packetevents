/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.retrooper.packetevents.test;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.component.builtin.item.PotDecorations;
import com.github.retrooper.packetevents.protocol.item.blocktransformer.BlockTransformer;
import com.github.retrooper.packetevents.protocol.item.blocktransformer.BlockTransformers;
import com.github.retrooper.packetevents.protocol.item.instrument.Instrument;
import com.github.retrooper.packetevents.protocol.item.instrument.StaticInstrument;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTLimiter;
import com.github.retrooper.packetevents.protocol.nbt.serializer.DefaultNBTSerializer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.protocol.world.Difficulty;
import com.github.retrooper.packetevents.protocol.world.WorldBlockPosition;
import com.github.retrooper.packetevents.protocol.world.blockentity.SignTextSlot;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.test.base.BaseDummyAPITest;
import com.github.retrooper.packetevents.util.mappings.SimpleRegistry;
import com.github.retrooper.packetevents.util.mappings.SynchronizedRegistriesHandler;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerRegistryData.RegistryElement;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 26.3 encodings that the item base component parse never exercises, the static
 * registries the API loads first (which a 26.3 default value once took down), and the
 * registry data a 26.3 client is sent on its first join.
 */
public class Protocol26_3Test extends BaseDummyAPITest {

    // the decorated pot's own default: four brick templates, each an optional item template
    // of item 1142 (a brick in 26.3), count 1 and an empty component patch
    private static final byte[] POT_DECORATIONS_26_3 = hex("01f608010000" + "01f608010000" + "01f608010000" + "01f608010000");
    // the same before 26.3: a list of four item ids, a brick standing for an undecorated side
    private static final byte[] POT_DECORATIONS_26_2 = hex("049e089e089e089e08");

    @Test
    @DisplayName("The registries the API loads at start initialize")
    public void staticRegistriesLoad() {
        assertDoesNotThrow(WrappedBlockState::ensureLoad);
        assertDoesNotThrow(SynchronizedRegistriesHandler::init);
        assertDoesNotThrow(PacketType::prepare);
    }

    @Test
    @DisplayName("The vanilla 26.3 block transformers sync through their codec")
    public void vanillaBlockTransformersSync() throws IOException {
        // the registry as ViaVersion 5.12.0 sends it to a 26.3 client of a 26.2 server,
        // which is the vanilla 26.3 registry: axe, hoe and shovel, each a list of transforms
        NBTCompound registry;
        try (InputStream stream = Protocol26_3Test.class.getResourceAsStream("/block_transformer_26_3.nbt")) {
            assertNotNull(stream);
            registry = (NBTCompound) DefaultNBTSerializer.INSTANCE.deserializeTag(
                    NBTLimiter.noop(), new DataInputStream(stream), true);
        }
        List<RegistryElement> elements = new ArrayList<>();
        for (Map.Entry<String, NBT> entry : registry.getTags().entrySet()) {
            elements.add(new RegistryElement(new ResourceLocation(entry.getKey()), entry.getValue()));
        }
        assertEquals(3, elements.size());

        SynchronizedRegistriesHandler.RegistryEntry<?> entry = SynchronizedRegistriesHandler
                .getRegistryEntry(BlockTransformers.getRegistry().getRegistryKey());
        assertNotNull(entry);
        SimpleRegistry<?> synced = entry.createFromElements(elements, PacketWrapper.createDummyWrapper(ClientVersion.V_26_3));

        BlockTransformer axe = (BlockTransformer) synced.getByName("minecraft:axe");
        assertNotNull(axe);
        assertEquals(0, axe.getId(ClientVersion.V_26_3));
        assertFalse(axe.getTransforms().isEmpty());
        BlockTransformer shovel = (BlockTransformer) synced.getById(ClientVersion.V_26_3, 2);
        assertNotNull(shovel);
        assertEquals(BlockTransformers.SHOVEL.getName(), shovel.getName());
    }

    @Test
    @DisplayName("Pot decorations are four optional item templates since 26.3")
    public void potDecorationsAreItemTemplates() {
        assertEquals(1142, ItemTypes.BRICK.getId(ClientVersion.V_26_3));

        PotDecorations decorations = read(ServerVersion.V_26_3, POT_DECORATIONS_26_3, PotDecorations::read);
        assertEquals(ItemTypes.BRICK, decorations.getBack());
        assertEquals(ItemTypes.BRICK, decorations.getFront());
        assertNotNull(decorations.getLeftStack());
        assertEquals(1, decorations.getLeftStack().getAmount());
        assertArrayEquals(POT_DECORATIONS_26_3, write(ServerVersion.V_26_3, decorations, PotDecorations::write));

        PotDecorations sparse = new PotDecorations(null, ItemTypes.ANGLER_POTTERY_SHERD, null, null);
        byte[] bytes = write(ServerVersion.V_26_3, sparse, PotDecorations::write);
        PotDecorations back = read(ServerVersion.V_26_3, bytes, PotDecorations::read);
        assertNull(back.getBackStack());
        assertEquals(ItemTypes.ANGLER_POTTERY_SHERD, back.getLeft());
        assertNull(back.getFront());
        assertArrayEquals(bytes, write(ServerVersion.V_26_3, back, PotDecorations::write));
    }

    @Test
    @DisplayName("Pot decorations stay a list of item ids before 26.3")
    public void potDecorationsStayItemIds() {
        PotDecorations decorations = read(ServerVersion.V_26_2, POT_DECORATIONS_26_2, PotDecorations::read);
        assertNull(decorations.getBack());
        assertNull(decorations.getFrontStack());
        assertArrayEquals(POT_DECORATIONS_26_2, write(ServerVersion.V_26_2, decorations, PotDecorations::write));
    }

    @Test
    @DisplayName("A respawn packet reads back at 26.3 with the previous game mode as a compact optional varint")
    public void respawnRoundTrips() {
        String world = "minecraft:survival";
        WorldBlockPosition death = new WorldBlockPosition(ResourceLocation.minecraft("overworld"), 10, 64, -3);
        WrapperPlayServerRespawn sent = new WrapperPlayServerRespawn(new DimensionTypeRef.IdRef(0), world,
                Difficulty.NORMAL, 42L, GameMode.CREATIVE, GameMode.SPECTATOR, false, true, (byte) 1, death, 20, 63);
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            sent.setServerVersion(ServerVersion.V_26_3);
            sent.setBuffer(buffer);
            sent.write();

            // dimension varint, the world name, the seed, then the two game modes: the current
            // as a varint and the previous as one varint of 0 for none, id + 1 otherwise, the
            // way ViaVersion's OPTIONAL_VAR_INT writes the byte a 26.2 backend sends
            byte[] bytes = ByteBufHelper.copyBytes(buffer);
            int gameModes = 1 + 1 + world.length() + 8;
            assertEquals(GameMode.CREATIVE.getId(), bytes[gameModes]);
            assertEquals(GameMode.SPECTATOR.getId() + 1, bytes[gameModes + 1]);

            WrapperPlayServerRespawn received = new WrapperPlayServerRespawn(new DimensionTypeRef.IdRef(0), null,
                    Difficulty.NORMAL, 0L, GameMode.SURVIVAL, null, false, false, (byte) 0, null, null, 0);
            received.setServerVersion(ServerVersion.V_26_3);
            received.setBuffer(buffer);
            received.read();

            assertEquals(0, ByteBufHelper.readableBytes(buffer), "every byte read");
            assertEquals(GameMode.CREATIVE, received.getGameMode());
            assertEquals(GameMode.SPECTATOR, received.getPreviousGameMode());
            assertNotNull(received.getLastDeathPosition());
            assertEquals(death.getWorld(), received.getLastDeathPosition().getWorld());
            assertEquals(death.getBlockPosition(), received.getLastDeathPosition().getBlockPosition());
            assertEquals(20, received.getPortalCooldown().orElse(-1));
            assertEquals(63, received.getSeaLevel());
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    @Test
    @DisplayName("A respawn packet with no previous game mode writes a zero for it since 26.3")
    public void respawnWithoutPreviousGameMode() {
        WrapperPlayServerRespawn sent = new WrapperPlayServerRespawn(new DimensionTypeRef.IdRef(0), "a",
                Difficulty.NORMAL, 0L, GameMode.ADVENTURE, null, false, false, (byte) 0, null, null, 0);
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            sent.setServerVersion(ServerVersion.V_26_3);
            sent.setBuffer(buffer);
            sent.write();
            byte[] bytes = ByteBufHelper.copyBytes(buffer);
            assertEquals(GameMode.ADVENTURE.getId(), bytes[11]);
            assertEquals(0, bytes[12]);

            sent.read();
            assertNull(sent.getPreviousGameMode());
            assertEquals(0, ByteBufHelper.readableBytes(buffer), "every byte read");
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    @Test
    @DisplayName("The sign text slot names the front as 1 and the back as 0")
    public void signTextSlotIsAVarInt() {
        assertArrayEquals(hex("01"), write(ServerVersion.V_26_3, SignTextSlot.FRONT, SignTextSlot::write));
        assertArrayEquals(hex("00"), write(ServerVersion.V_26_3, SignTextSlot.BACK, SignTextSlot::write));
        assertTrue(read(ServerVersion.V_26_3, hex("01"), SignTextSlot::read).isFront());
        assertFalse(SignTextSlot.ofFront(false).isFront());
    }

    @Test
    @DisplayName("An instrument carries its durability damage since 26.3")
    public void instrumentCarriesDurabilityDamage() {
        Instrument instrument = new StaticInstrument(null, Sounds.ITEM_GOAT_HORN_SOUND_0, 7f, 256f, 3, Component.text("horn"));
        byte[] direct = write(ServerVersion.V_26_3, instrument, Instrument::writeDirect);
        Instrument back = read(ServerVersion.V_26_3, direct, Instrument::readDirect);
        assertEquals(3, back.getDurabilityDamage());
        assertEquals(256f, back.getRange());

        byte[] legacy = write(ServerVersion.V_26_2, instrument, Instrument::writeDirect);
        assertEquals(direct.length - 1, legacy.length);
        assertEquals(0, read(ServerVersion.V_26_2, legacy, Instrument::readDirect).getDurabilityDamage());
    }

    private static <T> T read(ServerVersion version, byte[] bytes, Function<PacketWrapper<?>, T> reader) {
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            ByteBufHelper.writeBytes(buffer, bytes);
            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(buffer, version);
            T value = reader.apply(wrapper);
            assertEquals(bytes.length, ByteBufHelper.readerIndex(buffer), "every byte read");
            return value;
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private static <T> byte[] write(ServerVersion version, T value, Writer<T> writer) {
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            writer.write(PacketWrapper.createUniversalPacketWrapper(buffer, version), value);
            return ByteBufHelper.copyBytes(buffer);
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private static byte[] hex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    @FunctionalInterface
    private interface Writer<T> {

        void write(PacketWrapper<?> wrapper, T value);
    }
}
