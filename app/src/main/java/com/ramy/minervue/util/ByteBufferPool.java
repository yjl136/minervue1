package com.ramy.minervue.util;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Created by peter on 11/8/13.
 */
public class ByteBufferPool {

    private Comparator<ByteBuffer> comparator = new Comparator<ByteBuffer>() {

        @Override
        public int compare(ByteBuffer lhs, ByteBuffer rhs) {
            return rhs.capacity() - lhs.capacity();
        }

    };

    private PriorityQueue<ByteBuffer> queue = new PriorityQueue<ByteBuffer>(16, comparator);

    private int maxDequeued;

    private int dequeued = 0;

    public ByteBufferPool(int maxDequeued) {
        this.maxDequeued = maxDequeued;
    }

    public synchronized void fillPool(int count, int size) {
        for (int i = 0; i < count; ++i) {
            queue.add(ByteBuffer.wrap(new byte[size]));
        }
    }

    public synchronized ByteBuffer dequeue(int size) {
        if (queue.isEmpty() || queue.peek().capacity() < size) {
            if (dequeued >= maxDequeued) {
                return null;
            } else {
                ++dequeued;
                return ByteBuffer.wrap(new byte[size]);
            }
        } else {
            ++dequeued;
            return queue.poll();
        }
    }

    public synchronized void queue(ByteBuffer buffer) {
        if (buffer != null) {
            buffer.clear();
            queue.add(buffer);
            --dequeued;
            if (dequeued < 0) {
                dequeued = 0;
            }
        }
    }

}
