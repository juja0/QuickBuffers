/*-
 * #%L
 * robobuf-benchmarks
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

// automatically generated by the FlatBuffers compiler, do not modify

package protos.benchmarks.flatbuffers.fb;

import java.nio.*;
import java.lang.*;

import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class FooBarContainer extends Table {
  public static FooBarContainer getRootAsFooBarContainer(ByteBuffer _bb) { return getRootAsFooBarContainer(_bb, new FooBarContainer()); }
  public static FooBarContainer getRootAsFooBarContainer(ByteBuffer _bb, FooBarContainer obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public FooBarContainer __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public FooBar list(int j) { return list(new FooBar(), j); }
  public FooBar list(FooBar obj, int j) { int o = __offset(4); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int listLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }
  public boolean initialized() { int o = __offset(6); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }
  public short fruit() { int o = __offset(8); return o != 0 ? bb.getShort(o + bb_pos) : 0; }
  public String location() { int o = __offset(10); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer locationAsByteBuffer() { return __vector_as_bytebuffer(10, 1); }
  public ByteBuffer locationInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 10, 1); }

  public static int createFooBarContainer(FlatBufferBuilder builder,
      int listOffset,
      boolean initialized,
      short fruit,
      int locationOffset) {
    builder.startObject(4);
    FooBarContainer.addLocation(builder, locationOffset);
    FooBarContainer.addList(builder, listOffset);
    FooBarContainer.addFruit(builder, fruit);
    FooBarContainer.addInitialized(builder, initialized);
    return FooBarContainer.endFooBarContainer(builder);
  }

  public static void startFooBarContainer(FlatBufferBuilder builder) { builder.startObject(4); }
  public static void addList(FlatBufferBuilder builder, int listOffset) { builder.addOffset(0, listOffset, 0); }
  public static int createListVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startListVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static void addInitialized(FlatBufferBuilder builder, boolean initialized) { builder.addBoolean(1, initialized, false); }
  public static void addFruit(FlatBufferBuilder builder, short fruit) { builder.addShort(2, fruit, 0); }
  public static void addLocation(FlatBufferBuilder builder, int locationOffset) { builder.addOffset(3, locationOffset, 0); }
  public static int endFooBarContainer(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
  public static void finishFooBarContainerBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
  public static void finishSizePrefixedFooBarContainerBuffer(FlatBufferBuilder builder, int offset) { builder.finishSizePrefixed(offset); }
}

