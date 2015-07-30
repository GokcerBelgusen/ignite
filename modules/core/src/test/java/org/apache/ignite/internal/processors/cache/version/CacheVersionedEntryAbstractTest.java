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

package org.apache.ignite.internal.processors.cache.version;

import org.apache.ignite.*;
import org.apache.ignite.cache.*;
import org.apache.ignite.cache.version.*;
import org.apache.ignite.internal.processors.cache.*;

import javax.cache.*;
import javax.cache.processor.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Versioned entry abstract test.
 */
public abstract class CacheVersionedEntryAbstractTest extends GridCacheAbstractSelfTest {
    /** Entries number to store in a cache. */
    private static final int ENTRIES_NUM = 1000;

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 2;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        Cache<Integer, String> cache = grid(0).cache(null);

        for (int i = 0 ; i < ENTRIES_NUM; i++)
            cache.put(i, "value_" + i);
    }

    /**
     * @throws Exception If failed.
     */
    public void testInvoke() throws Exception {
        Cache<Integer, String> cache = grid(0).cache(null);

        final AtomicBoolean invoked = new AtomicBoolean(false);

        cache.invoke(100, new EntryProcessor<Integer, String, Object>() {
            @Override public Object process(MutableEntry<Integer, String> entry, Object... arguments)
                throws EntryProcessorException {

                invoked.set(true);

                VersionedEntry<Integer, String> verEntry = entry.unwrap(VersionedEntry.class);

                checkVersionedEntry(verEntry);

                return entry;
            }
        });

        assertTrue(invoked.get());
    }

    /**
     * @throws Exception If failed.
     */
    public void testInvokeAll() throws Exception {
        Cache<Integer, String> cache = grid(0).cache(null);

        Set<Integer> keys = new HashSet<>();

        for (int i = 0; i < ENTRIES_NUM; i++)
            keys.add(i);

        final AtomicInteger invoked = new AtomicInteger();

        cache.invokeAll(keys, new EntryProcessor<Integer, String, Object>() {
            @Override public Object process(MutableEntry<Integer, String> entry, Object... arguments)
                throws EntryProcessorException {

                invoked.incrementAndGet();

                VersionedEntry<Integer, String> verEntry = entry.unwrap(VersionedEntry.class);

                checkVersionedEntry(verEntry);

                return null;
            }
        });

        assert invoked.get() > 0;
    }

    /**
     * @throws Exception If failed.
     */
    public void testRandomEntry() throws Exception {
        IgniteCache<Integer, String> cache = grid(0).cache(null);

        for (int i = 0; i < 5; i++)
            checkVersionedEntry(cache.randomEntry().unwrap(VersionedEntry.class));
    }

    /**
     * @throws Exception If failed.
     */
    public void testIterator() throws Exception {
        IgniteCache<Integer, String> cache = grid(0).cache(null);

        Iterator<Cache.Entry<Integer, String>> entries = cache.iterator();

        while (entries.hasNext())
            checkVersionedEntry(entries.next().unwrap(VersionedEntry.class));
    }

    /**
     * @throws Exception If failed.
     */
    public void testLocalPeek() throws Exception {
        IgniteCache<Integer, String> cache = grid(0).cache(null);

        Iterable<Cache.Entry<Integer, String>> entries = offheapTiered(cache) ?
            cache.localEntries(CachePeekMode.SWAP, CachePeekMode.OFFHEAP) :
            cache.localEntries(CachePeekMode.ONHEAP);

        for (Cache.Entry<Integer, String> entry : entries)
            checkVersionedEntry(entry.unwrap(VersionedEntry.class));
    }

    /**
     * @throws Exception If failed.
     */
    public void testVersionComparision() throws Exception {
        IgniteCache<Integer, String> cache = grid(0).cache(null);

        VersionedEntry<String, Integer> ver1 = cache.invoke(100,
            new EntryProcessor<Integer, String, VersionedEntry<String, Integer>>() {
                @Override public VersionedEntry<String, Integer> process(MutableEntry<Integer, String> entry,
                    Object... arguments) throws EntryProcessorException {
                        return entry.unwrap(VersionedEntry.class);
                    }
            });

        cache.put(100, "new value 100");

        VersionedEntry<String, Integer> ver2 = cache.invoke(100,
            new EntryProcessor<Integer, String, VersionedEntry<String, Integer>>() {
                @Override public VersionedEntry<String, Integer> process(MutableEntry<Integer, String> entry,
                    Object... arguments) throws EntryProcessorException {
                        return entry.unwrap(VersionedEntry.class);
                    }
            });

        assert VersionedEntry.VERSIONS_COMPARATOR.compare(ver1, ver2) < 0;
    }

    /**
     * @param entry Versioned entry.
     */
    private void checkVersionedEntry(VersionedEntry<Integer, String> entry) {
        assertNotNull(entry);

        assert entry.topologyVersion() > 0;
        assert entry.order() > 0;
        assert entry.nodeOrder() > 0;
        assert entry.globalTime() > 0;

        assertNotNull(entry.getKey());
        assertNotNull(entry.getValue());
    }
}
