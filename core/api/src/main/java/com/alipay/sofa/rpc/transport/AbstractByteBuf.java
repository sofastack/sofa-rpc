/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.transport;

import com.alipay.sofa.rpc.common.annotation.Unstable;

/**
 * <p>ByteBuf的一个抽象，这样可以隔离各种Bytebuf</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Unstable
public abstract class AbstractByteBuf {

    /* public abstract int capacity();

    public abstract AbstractByteBuf capacity(int newCapacity);
    */
    public abstract int readerIndex();

    public abstract AbstractByteBuf readerIndex(int readerIndex);

    public abstract int writerIndex();

    public abstract AbstractByteBuf writerIndex(int writerIndex);

    /*
    public abstract AbstractByteBuf setIndex(int readerIndex, int writerIndex);
    */
    public abstract int readableBytes();

    /*
    public abstract int writableBytes();

    public abstract int maxWritableBytes();

    public abstract boolean isReadable();

    public abstract boolean isReadable(int size);

    public abstract boolean isWritable();

    public abstract boolean isWritable(int size);

    public abstract AbstractByteBuf clear();

    public abstract AbstractByteBuf markReaderIndex();

    public abstract AbstractByteBuf resetReaderIndex();

    public abstract AbstractByteBuf markWriterIndex();

    public abstract AbstractByteBuf resetWriterIndex();

    public abstract AbstractByteBuf discardReadBytes();

    public abstract AbstractByteBuf discardSomeReadBytes();

    public abstract AbstractByteBuf ensureWritable(int minWritableBytes);

    public abstract int ensureWritable(int minWritableBytes, boolean force);

    public abstract boolean getBoolean(int index);

    public abstract byte getByte(int index);

    public abstract short getUnsignedByte(int index);

    public abstract short getShort(int index);

    public abstract short getShortLE(int index);

    public abstract int getUnsignedShort(int index);

    public abstract int getUnsignedShortLE(int index);

    public abstract int getMedium(int index);

    public abstract int getMediumLE(int index);

    public abstract int getUnsignedMedium(int index);

    public abstract int getUnsignedMediumLE(int index);

    public abstract int getInt(int index);

    public abstract int getIntLE(int index);

    public abstract long getUnsignedInt(int index);

    public abstract long getUnsignedIntLE(int index);

    public abstract long getLong(int index);

    public abstract long getLongLE(int index);

    public abstract char getChar(int index);

    public abstract float getFloat(int index);

    public abstract double getDouble(int index);

    public abstract AbstractByteBuf getBytes(int index, AbstractByteBuf dst);

    public abstract AbstractByteBuf getBytes(int index, AbstractByteBuf dst, int length);

    public abstract AbstractByteBuf getBytes(int index, AbstractByteBuf dst, int dstIndex, int length);

    public abstract AbstractByteBuf getBytes(int index, byte[] dst);

    public abstract AbstractByteBuf getBytes(int index, byte[] dst, int dstIndex, int length);

    public abstract AbstractByteBuf getBytes(int index, OutputStream out, int length) throws IOException;

    public abstract int getBytes(int index, GatheringByteChannel out, int length) throws IOException;

    public abstract int getBytes(int index, FileChannel out, long position, int length) throws IOException;

    public abstract CharSequence getCharSequence(int index, int length, Charset charset);

    public abstract AbstractByteBuf setBoolean(int index, boolean value);
    */
    public abstract AbstractByteBuf setByte(int index, int value);

    /*
    public abstract AbstractByteBuf setShort(int index, int value);

    public abstract AbstractByteBuf setShortLE(int index, int value);

    public abstract AbstractByteBuf setMedium(int index, int value);

    public abstract AbstractByteBuf setMediumLE(int index, int value);

    public abstract AbstractByteBuf setInt(int index, int value);

    public abstract AbstractByteBuf setIntLE(int index, int value);

    public abstract AbstractByteBuf setLong(int index, long value);

    public abstract AbstractByteBuf setLongLE(int index, long value);

    public abstract AbstractByteBuf setChar(int index, int value);

    public abstract AbstractByteBuf setFloat(int index, float value);

    public abstract AbstractByteBuf setDouble(int index, double value);

    public abstract AbstractByteBuf setBytes(int index, AbstractByteBuf src, int length);

    public abstract AbstractByteBuf setBytes(int index, AbstractByteBuf src, int srcIndex, int length);
    */
    public abstract AbstractByteBuf setBytes(int index, byte[] src);

    /*
    public abstract AbstractByteBuf setBytes(int index, byte[] src, int srcIndex, int length);

    public abstract AbstractByteBuf setBytes(int index, AbstractByteBuf src);

    public abstract int setBytes(int index, InputStream in, int length) throws IOException;

    public abstract int setBytes(int index, ScatteringByteChannel in, int length) throws IOException;

    public abstract int setBytes(int index, FileChannel in, long position, int length) throws IOException;

    public abstract AbstractByteBuf setZero(int index, int length);

    public abstract int setCharSequence(int index, CharSequence sequence, Charset charset);
    */
    public abstract boolean readBoolean();

    public abstract byte readByte();

    /*
    public abstract short readUnsignedByte();
    */
    public abstract short readShort();

    /*
    public abstract short readShortLE();

    public abstract int readUnsignedShort();

    public abstract int readUnsignedShortLE();

    public abstract int readMedium();

    public abstract int readMediumLE();

    public abstract int readUnsignedMedium();

    public abstract int readUnsignedMediumLE();
    */
    public abstract int readInt();

    /*
    public abstract int readIntLE();

    public abstract long readUnsignedInt();

    public abstract long readUnsignedIntLE();
    */
    public abstract long readLong();

    /*
    public abstract long readLongLE();
    */
    public abstract char readChar();

    public abstract float readFloat();

    public abstract double readDouble();

    /*
    public abstract AbstractByteBuf readBytes(int length);

    public abstract AbstractByteBuf readSlice(int length);

    public abstract AbstractByteBuf readRetainedSlice(int length);

    public abstract AbstractByteBuf readBytes(AbstractByteBuf dst, int length);

    public abstract AbstractByteBuf readBytes(AbstractByteBuf dst, int dstIndex, int length);
    */
    public abstract AbstractByteBuf readBytes(byte[] dst);

    /*
    public abstract AbstractByteBuf readBytes(byte[] dst, int dstIndex, int length);

    public abstract AbstractByteBuf readBytes(AbstractByteBuf dst);

    public abstract AbstractByteBuf readBytes(OutputStream out, int length) throws IOException;

    public abstract int readBytes(GatheringByteChannel out, int length) throws IOException;

    public abstract CharSequence readCharSequence(int length, Charset charset);

    public abstract int readBytes(FileChannel out, long position, int length) throws IOException;

    public abstract AbstractByteBuf skipBytes(int length);
    */
    public abstract AbstractByteBuf writeBoolean(boolean value);

    public abstract AbstractByteBuf writeByte(int value);

    public abstract AbstractByteBuf writeShort(int value);

    /*
    public abstract AbstractByteBuf writeShortLE(int value);

    public abstract AbstractByteBuf writeMedium(int value);

    public abstract AbstractByteBuf writeMediumLE(int value);
    */
    public abstract AbstractByteBuf writeInt(int value);

    /*
    public abstract AbstractByteBuf writeIntLE(int value);
    */
    public abstract AbstractByteBuf writeLong(long value);

    /*
    public abstract AbstractByteBuf writeLongLE(long value);
    */
    public abstract AbstractByteBuf writeChar(int value);

    public abstract AbstractByteBuf writeFloat(float value);

    public abstract AbstractByteBuf writeDouble(double value);

    /*
    public abstract AbstractByteBuf writeBytes(AbstractByteBuf src, int length);

    public abstract AbstractByteBuf writeBytes(AbstractByteBuf src, int srcIndex, int length);
    */
    public abstract AbstractByteBuf writeBytes(byte[] src);

    /*
    public abstract AbstractByteBuf writeBytes(byte[] src, int srcIndex, int length);

    public abstract AbstractByteBuf writeBytes(AbstractByteBuf src);

    public abstract int writeBytes(InputStream in, int length) throws IOException;

    public abstract int writeBytes(ScatteringByteChannel in, int length) throws IOException;

    public abstract int writeBytes(FileChannel in, long position, int length) throws IOException;

    public abstract AbstractByteBuf writeZero(int length);

    public abstract int writeCharSequence(CharSequence sequence, Charset charset);

    public abstract int indexOf(int fromIndex, int toIndex, byte value);

    public abstract int bytesBefore(byte value);

    public abstract int bytesBefore(int length, byte value);

    public abstract int bytesBefore(int index, int length, byte value);

    public abstract AbstractByteBuf copy();

    public abstract AbstractByteBuf copy(int index, int length);

    public abstract AbstractByteBuf slice();

    public abstract AbstractByteBuf retainedSlice();
    */
    public abstract AbstractByteBuf slice(int index, int length);

    /*
    public abstract AbstractByteBuf retainedSlice(int index, int length);

    public abstract AbstractByteBuf duplicate();

    public abstract AbstractByteBuf retainedDuplicate();

    public abstract int nioBufferCount();

    public abstract AbstractByteBuf nioBuffer();

    public abstract AbstractByteBuf nioBuffer(int index, int length);

    public abstract AbstractByteBuf internalNioBuffer(int index, int length);

    public abstract AbstractByteBuf[] nioBuffers();

    public abstract AbstractByteBuf[] nioBuffers(int index, int length);

    public abstract boolean hasArray(); */

    public abstract byte[] array();

    /*
    public abstract int arrayOffset();

    public abstract boolean hasMemoryAddress();

    public abstract long memoryAddress();

    public abstract String toString(Charset charset);

    public abstract String toString(int index, int length, Charset charset);

    public abstract int hashCode();

    public abstract boolean equals(Object obj);

    public abstract int compareTo(AbstractByteBuf buffer);

    public abstract String toString();
    */
    public abstract AbstractByteBuf retain(int increment);

    public abstract int refCnt();

    public abstract AbstractByteBuf retain();

    /*
    public abstract AbstractByteBuf touch();

    public abstract AbstractByteBuf touch(Object hint);
    */
    public abstract boolean release(int decrement);

    public abstract boolean release();
}
