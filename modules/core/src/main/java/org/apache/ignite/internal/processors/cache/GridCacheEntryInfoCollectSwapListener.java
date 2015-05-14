/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.*;
import org.jsr166.*;

import java.util.*;
import java.util.concurrent.locks.*;

/**
 *
 */
public class GridCacheEntryInfoCollectSwapListener implements GridCacheSwapListener {
    /** */
    private final Map<KeyCacheObject, GridCacheEntryInfo> swappedEntries = new ConcurrentHashMap8<>();

    private final ConcurrentHashMap8<KeyCacheObject, GridCacheEntryInfo>  notFinishedSwappedEntries = new ConcurrentHashMap8<>();


    final Lock lock = new ReentrantLock();
    final Condition emptyCond  = lock.newCondition();

    /** */
    private final IgniteLogger log;

    /**
     * @param log Logger.
     */
    public GridCacheEntryInfoCollectSwapListener(IgniteLogger log) {
        this.log = log;
    }

    /**
     * Wait until all entries finish unswapping.
     */
    public void waitUnswapFinished() {
        lock.lock();
        try{
            if (notFinishedSwappedEntries.size() != 0)
                try {
                    emptyCond.await();
                }
                catch (InterruptedException e) {
                    // No-op.
                }
        } finally {
            lock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public void onEntryUnswapping(int part, KeyCacheObject key, GridCacheSwapEntry swapEntry) throws IgniteCheckedException {
        if (log.isDebugEnabled())
            log.debug("Received unswapped event for key: " + key);

        assert key != null;
        assert swapEntry != null;

        GridCacheEntryInfo info = new GridCacheEntryInfo();

        info.key(key);
        info.ttl(swapEntry.ttl());
        info.expireTime(swapEntry.expireTime());
        info.version(swapEntry.version());
        info.value(swapEntry.value());

        notFinishedSwappedEntries.put(key, info);
    }

    /** {@inheritDoc} */
    @Override public void onEntryUnswapped(int part,
        KeyCacheObject key,
        GridCacheSwapEntry swapEntry)
    {
        GridCacheEntryInfo info = notFinishedSwappedEntries.remove(key);

        assert info != null;

        swappedEntries.put(key, info);

        lock.lock();
        try{
            if (notFinishedSwappedEntries.size() == 0)
                emptyCond.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return Entries, received by listener.
     */
    public Collection<GridCacheEntryInfo> entries() {
        return swappedEntries.values();
    }
}
