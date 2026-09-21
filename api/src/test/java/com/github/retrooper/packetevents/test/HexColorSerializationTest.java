/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
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

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.test.base.BaseDummyAPITest;
import com.github.retrooper.packetevents.util.adventure.AdventureNBTSerializer;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class HexColorSerializationTest extends BaseDummyAPITest {

    @ParameterizedTest
    @ValueSource(ints = {0x000001, 0x00000F, 0x0000F0, 0x12AB0F, 0xABCDEF, 0xF1E89A, 0xFFFFFE})
    @DisplayName("A hex color is written as the formatter wrote it: '#' and six upper-case digits")
    public void testHexColorMatchesFormatter(int rgb) {
        AdventureNBTSerializer serializer = AdventureSerializer.serializer(ClientVersion.V_1_21_6).nbt();
        PacketWrapper<?> wrapper = PacketWrapper.createDummyWrapper(ClientVersion.V_1_21_6);
        NBT serialized = serializer.serialize(Component.text("x", TextColor.color(rgb)), wrapper);

        assertInstanceOf(NBTCompound.class, serialized);
        NBT color = ((NBTCompound) serialized).getTagOrNull("color");
        assertInstanceOf(NBTString.class, color);
        assertEquals(String.format(Locale.ROOT, "%c%06X", TextColor.HEX_CHARACTER, rgb), ((NBTString) color).getValue());
    }
}
