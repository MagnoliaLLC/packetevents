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

package com.github.retrooper.packetevents.protocol.world.blockentity;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;

/**
 * The side of a sign a text edit addresses, as the sign editor packets carry it since 26.3:
 * a varint slot in place of the "is front text" boolean of 1.20 to 26.2, with the back at 0
 * and the front at 1.
 *
 * @versions 26.3+
 */
public enum SignTextSlot {

    BACK,
    FRONT;

    private static final SignTextSlot[] VALUES = values();

    public static SignTextSlot ofFront(boolean front) {
        return front ? FRONT : BACK;
    }

    public static SignTextSlot read(PacketWrapper<?> wrapper) {
        int id = wrapper.readVarInt();
        if (id < 0 || id >= VALUES.length) {
            throw new IllegalStateException("Unknown sign text slot " + id);
        }
        return VALUES[id];
    }

    public static void write(PacketWrapper<?> wrapper, SignTextSlot slot) {
        wrapper.writeVarInt(slot.ordinal());
    }

    public boolean isFront() {
        return this == FRONT;
    }
}
