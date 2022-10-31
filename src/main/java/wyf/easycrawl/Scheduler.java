package wyf.easycrawl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 调度器
 */
public class Scheduler {
    Logger logger = LoggerFactory.getLogger(Scheduler.class);
    ConcurrentSkipListSet<String> visited = new ConcurrentSkipListSet<>();
    ThreadPoolExecutor poolExecutor;
    volatile boolean running;
    public RequestHandler downloader;

    public Scheduler(RequestHandler downloader) {
        poolExecutor = new ThreadPoolExecutor(10, 40, 1000 * 20, TimeUnit.MICROSECONDS, new LinkedBlockingDeque<>());
        this.downloader = downloader;
    }

    String removeHash(String url) {
        //去掉URL中的hash
        int hashIndex = url.lastIndexOf('#');
        if (hashIndex == -1) {
            return url;
        }
        return url.substring(0, hashIndex);
    }

    //供外部调用，判断是否已经访问过某个url
    public boolean hasVisited(String url) {
        url = removeHash(url);
        return visited.contains(url);
    }

    //设置某个url已经访问过，用于禁止访问某些url
    public void putVisited(String url) {
        url = removeHash(url);
        visited.add(url);
    }

    /**
     * 此函数接受url和handler有两种作用：
     * 1、先判断是否访问过，如果没有访问过，免除了创建Request对象的过程
     * 2、简化代码，创建Request并设置值在此函数中完成
     */
    public void pushRequest(String url, ResponseHandler handler) {
        if (visited.contains(url)) return;
        visited.add(url);
        Request req = new Request();
        pushForce(req);
    }

    public void pushRequest(Request req) {
        if (visited.contains(req.getUrl())) return;
        logger.info(req.getUrl() + " 被添加到队列");
        visited.add(req.getUrl());
        pushForce(req);
    }

    public void pushForce(Request req) {
        poolExecutor.execute(() -> {
            downloader.handle(req);
        });
    }

    public void pushRunnable(Runnable r) {
        poolExecutor.execute(r);
    }

    public void shutdown() {
        this.running = false;
    }

    public void run() {
        running = true;
        int waitCount = 0;
        while (running) {
            if (poolExecutor.getQueue().size() == 0 && poolExecutor.getActiveCount() == 0) {
                //只要没活干waitCount次，就会跳出循环
                waitCount++;
                if (waitCount > 3)
                    break;
            } else {
                waitCount = 0;
            }
            logger.info("===============");
            logger.info("活跃进程数" + poolExecutor.getActiveCount());
            logger.info("剩余任务数" + poolExecutor.getQueue().size()
                    + "+已完成任务数" + poolExecutor.getCompletedTaskCount()
                    + "=任务总数" + poolExecutor.getTaskCount());
            logger.info("当前进程池进程数" + poolExecutor.getPoolSize());
            logger.info("进程池核心进程数" + poolExecutor.getCorePoolSize());
            logger.info("已经达到的最大进程数" + poolExecutor.getLargestPoolSize());
            logger.info("进程上限" + poolExecutor.getMaximumPoolSize());
            logger.info("保持活跃时间" + poolExecutor.getKeepAliveTime(TimeUnit.SECONDS));
            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        poolExecutor.shutdown();
        poolExecutor.shutdownNow();
        logger.info("运行结束....");
    }
}
