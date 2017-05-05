package org.apache.ignite.cache.database.pagemem;

import java.nio.ByteBuffer;
import org.apache.ignite.internal.mem.DirectMemoryProvider;
import org.apache.ignite.internal.mem.unsafe.UnsafeMemoryProvider;
import org.apache.ignite.internal.pagemem.FullPageId;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.database.CheckpointLockStateChecker;
import org.apache.ignite.internal.processors.cache.database.IgniteCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.cache.database.pagemem.PageMemoryEx;
import org.apache.ignite.internal.processors.cache.database.pagemem.PageMemoryImpl;
import org.apache.ignite.internal.processors.cache.database.wal.FileWriteAheadLogManager;
import org.apache.ignite.internal.processors.database.BPlusTreeReuseSelfTest;
import org.apache.ignite.internal.util.lang.GridInClosure3X;
import org.apache.ignite.internal.util.typedef.CIX3;
import org.apache.ignite.testframework.junits.GridTestKernalContext;

/**
 *
 */
public class BPlusTreeReuseListPageMemoryImplSelfTest extends BPlusTreeReuseSelfTest {
    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        System.setProperty(FileWriteAheadLogManager.IGNITE_PDS_WAL_MODE, "LOG_ONLY");

        super.beforeTest();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        System.clearProperty(FileWriteAheadLogManager.IGNITE_PDS_WAL_MODE);
    }

    /** {@inheritDoc} */
    @Override protected PageMemory createPageMemory() throws Exception {
        long[] sizes = new long[CPUS + 1];

        for (int i = 0; i < sizes.length; i++)
            sizes[i] = 1024 * MB / CPUS;

        sizes[CPUS] = 10 * MB;

        DirectMemoryProvider provider = new UnsafeMemoryProvider(sizes);

        GridCacheSharedContext<Object, Object> sharedCtx = new GridCacheSharedContext<>(
            new GridTestKernalContext(log),
            null,
            null,
            null,
            new NoOpPageStoreManager(),
            new NoOpWALManager(),
            new IgniteCacheDatabaseSharedManager(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        PageMemory mem = new PageMemoryImpl(provider, sharedCtx, PAGE_SIZE,
            new CIX3<FullPageId, ByteBuffer, Integer>() {
            @Override public void applyx(FullPageId fullPageId, ByteBuffer byteBuf, Integer tag) {
                assert false : "No evictions should happen during the test";
            }
        }, new GridInClosure3X<Long, FullPageId, PageMemoryEx>() {
            @Override public void applyx(Long page, FullPageId fullPageId, PageMemoryEx pageMem) {
            }
        }, new CheckpointLockStateChecker() {
            @Override public boolean checkpointLockIsHeldByThread() {
                return true;
            }
        });

        mem.start();

        return mem;
    }

    /** {@inheritDoc} */
    @Override protected long acquiredPages() {
        return ((PageMemoryImpl)pageMem).acquiredPages();
    }
}