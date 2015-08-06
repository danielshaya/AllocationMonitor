import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Created by daniel on 22/07/2015.
 * This class allows a user to receive callbacks if
 * threads are destroyed or created.
 * Its primary function is to alert the user if any
 * thread has exceeded a specified amount of allocation.
 */
public class AllocationsMonitor {
  public static final Consumer<Thread> EMPTY = a -> { };
  public static final BiConsumer<Thread, Long> BI_EMPTY =
      (a, b) -> { };
  private final Map<Thread, AllocationMonitorSingleThread> ams;
  private volatile Consumer<Thread> threadCreated = EMPTY;
  private volatile Consumer<Thread> threadDied =
      EMPTY;
  private volatile ByteWatch byteWatch = new ByteWatch(
      BI_EMPTY, Long.MAX_VALUE
  );

  private static class ByteWatch
      implements BiConsumer<Thread, Long>, Predicate<Long>{
    private final long threshold;
    private final BiConsumer<Thread, Long> byteWatch;

    public ByteWatch(BiConsumer<Thread, Long> byteWatch, long threshold) {
      this.byteWatch = byteWatch;
      this.threshold = threshold;
    }

    public void accept(Thread thread, Long currentBytes) {
      byteWatch.accept(thread, currentBytes);
    }

    public boolean test(Long currentBytes) {
      return threshold < currentBytes;
    }
  }

  private final ScheduledExecutorService monitorService =
      Executors.newSingleThreadScheduledExecutor();

  public AllocationsMonitor() {
    // do this first so that the worker thread is not considered
    // a "newly created" thread
    monitorService.scheduleAtFixedRate(
        this::checkThreads, 500, 500, TimeUnit.MILLISECONDS);

    ams = Thread.getAllStackTraces()
        .keySet()
        .stream()
        .map(AllocationMonitorSingleThread::new)
        .collect(Collectors.toConcurrentMap(
            AllocationMonitorSingleThread::getThread,
            (AllocationMonitorSingleThread am) -> am));
    // Heinz: Streams make sense, right? ;-)
  }

  public void onThreadCreated(Consumer<Thread> action) {
    threadCreated = action;
  }

  public void onThreadDied(Consumer<Thread> action) {
    threadDied = action;
  }

  public void onByteWatch(
      BiConsumer<Thread, Long> action, long threshold) {
    this.byteWatch = new ByteWatch(action, threshold);
  }

  public void shutdown() {
    monitorService.shutdown();
  }

  public void forEach(Consumer<AllocationMonitorSingleThread> c) {
    ams.values().forEach(c);
  }

  public void printAllAllocations() {
    forEach(System.out::println);
  }

  public void reset() {
    forEach(AllocationMonitorSingleThread::reset);
  }

  private void checkThreads() {
    Set<Thread> oldThreads = ams.keySet();
    Set<Thread> newThreads = Thread.getAllStackTraces().keySet();

    Set<Thread> diedThreads = new HashSet<>(oldThreads);
    diedThreads.removeAll(newThreads);

    Set<Thread> createdThreads = new HashSet<>(newThreads);
    createdThreads.removeAll(oldThreads);

    diedThreads.forEach(this::threadDied);
    createdThreads.forEach(this::threadCreated);
    ams.values().forEach(this::bytesWatch);
  }

  private void threadCreated(Thread t) {
    ams.put(t, new AllocationMonitorSingleThread(t));
    threadCreated.accept(t);
  }

  private void threadDied(Thread t) {
    threadDied.accept(t);
  }

  private void bytesWatch(AllocationMonitorSingleThread am) {
    ByteWatch bw = byteWatch;
    long bytesAllocated = am.calculateAllocations();
    if (bw.test(bytesAllocated)) {
      bw.accept(am.getThread(), bytesAllocated);
    }
  }
}