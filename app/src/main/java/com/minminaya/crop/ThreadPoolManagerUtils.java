package com.minminaya.crop;

import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>线程池管理类</p>
 *
 * @time Created by 2018/6/26 17:54
 */
public class ThreadPoolManagerUtils {
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // 核心线程数
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;

    // 线程池最大线程数
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    // 非核心线程闲置时超时0s
    private static final long KEEP_ALIVE = 1L;

    // 确保该类只有一个实例对象
    private static ThreadPoolManagerUtils sInstance;
    // 线程池的对象
    private ThreadPoolExecutor executor;

    private ThreadPoolManagerUtils() {
    }

    public synchronized static ThreadPoolManagerUtils getInstance() {
        if (sInstance == null) {
            sInstance = new ThreadPoolManagerUtils();
        }
        return sInstance;
    }

    /**
     * 使用线程池，线程池中线程的创建完全是由线程池自己来维护的，我们不需要创建任何的线程
     *
     * @param runnable 在线程池里面运行的线程
     */
    public void execute(Runnable runnable) {
        if (executor == null) {
            executor =
                new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Executors.defaultThreadFactory(),
                    new ThreadPoolExecutor.AbortPolicy());
        }
        executor.execute(runnable);// 添加任务
    }

    /**
     * 移除指定的线程
     *
     * @param runnable 指定的线程
     */
    public void cancel(Runnable runnable) {
        if (runnable != null) {
            executor.getQueue().remove(runnable);// 移除任务
        }
    }
}
