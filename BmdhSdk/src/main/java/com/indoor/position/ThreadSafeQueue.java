package com.indoor.position;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;

import java.util.concurrent.locks.ReentrantLock;


/**
 * A threadsafe queue.
 * */
class ThreadSafeQueue<T> {

    private final EvictingQueue<T> q;
    private final ReentrantLock lock = new ReentrantLock(true);

    ThreadSafeQueue(int size) {
        this.q = EvictingQueue.create(size);
    }

    void add(T e) {
        lock.lock();
        try {
            this.q.add(e);
        } finally {
            lock.unlock();
        }
    }

    void clear() {
        lock.lock();
        try {
            this.q.clear();
        } finally {
            lock.unlock();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    ImmutableList<T> toList() {
        lock.lock();
        try {
            return this.q.stream().collect(ImmutableList.toImmutableList());
        } finally {
            lock.unlock();
        }
    }
}