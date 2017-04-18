# ByteWatcher

ByteWatcher allows you to programmatically monitor the number of bytes allocated by your program.

It is especially useful for embedding into regression tests when allocation is a critical factor. This will typically be the case for low latency applications.

You can monitor all the threads in your application and are alerted when an exceeded number of bytes are allocated.

## Getting Started:

If you want the latest release binary to add to your project just go to the [latest release in GitHub](https://github.com/danielshaya/org.octtech.bw.ByteWatcher/releases) and download the jar file.

Alternatively you can download the source code from GitHub and build using Maven.

To be honest, given the amount of code, even copy and paste should work fine.

## Example Usage:

This code will print out a warning message if more than 1MB of data has been allocated by a single thread:
```java
long limit = 1<<20;
ByteWatcher by = new ByteWatcher();
bw.onByteWatch((t, size) ->
        System.out.printf("%s exceeded limit: %d using: %d%n",
            t.getName(), limit, size)
        , limit);
```
Example output:
```
Allocating Thread exceeded limit: 1048576 using: 35193128
Allocating Thread exceeded limit: 1048576 using: 64595336
Allocating Thread exceeded limit: 1048576 using: 80595336
Allocating Thread exceeded limit: 1048576 using: 97217520
Allocating Thread exceeded limit: 1048576 using: 101457304
Allocating Thread exceeded limit: 1048576 using: 132750240
Allocating Thread exceeded limit: 1048576 using: 148750240
Allocating Thread exceeded limit: 1048576 using: 163709440
```

You can also be alerted when a thread is created or destroyed:

This code will print out all the threads when a new thread is created
```java
ByteWatcher bw = new ByteWatcher();
bw.onThreadCreated(System.out::println);
```

This code will print out all the threads when a thread is destroyed
```java
ByteWatcher bw = new ByteWatcher();
bw.onThreadDied(System.out::println);
```

The class has a useful utility method which allows you to see all the allocation at any point in time
```java
ByteWatcher bw = new ByteWatcher();
bw.printAllAllocations();
```
Example output:
```
Monitor Ctrl-Break allocated 0
Signal Dispatcher allocated 0
Reference Handler allocated 0
main allocated 373176
pool-1-thread-1 allocated 0
Finalizer allocated 0
```

BytesWatcher will by default sample the threads every 500 milliseconds.  This can be overridden with the vm option `SamplingIntervalMillis`.

Each sampling costs normally 336 bytes.  This is accounted for in the calculation so is not attributed to your program.  It has been noted that on occasion this number can vary slightly.  Best efforts have been made to minimise the impact this might have on a client program by running a calibration routine when ByteWatcher starts.  However unless you are quite literally looking for zero allocation the effect will be negligible.  Even in a zero allocation environment it shouldn't have an impact and can be completely mitigated by running your program with -Xcomp.




