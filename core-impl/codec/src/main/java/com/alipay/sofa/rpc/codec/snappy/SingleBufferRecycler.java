package com.alipay.sofa.rpc.codec.snappy;

class SingleBufferRecycler {
    private final static int MIN_ENCODING_BUFFER = 4000;
    private byte[] encodingBuffer;

    SingleBufferRecycler() {
    }

    byte[] allocEncodingBuffer(int minSize) {
        byte[] buf = encodingBuffer;
        if (buf == null || buf.length < minSize) {
            buf = new byte[Math.max(minSize, MIN_ENCODING_BUFFER)];
        } else {
            encodingBuffer = null;
        }
        return buf;
    }

    void releaseEncodeBuffer(byte[] buffer) {
        if (encodingBuffer == null || buffer.length > encodingBuffer.length) {
            encodingBuffer = buffer;
        }
    }
}