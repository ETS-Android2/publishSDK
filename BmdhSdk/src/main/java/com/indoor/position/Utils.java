package com.indoor.position;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Utils {

    static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    private Utils() {
    }
}