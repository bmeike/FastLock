## Introduction

As I relate in the preface to my most recent book, Addison Wesley’s [Android Concurrency](https://www.amazon.com/Android-Concurrency-Deep-Dive/dp/0134177436/ref=sr_1_1"), one of my early introductions to advanced concurrent programming was the double checked lock (DCL). A very supportive interviewer guided me into inventing the pattern for myself, during a job interview. The year was probably 1999 and I got the job.

It was less than year later that I stumbled into articles, first by Bill Pugh, then Doug Lea, Josh Bloch and Brian Goetz, all pointing out that the idiom was flawed and would not work, in Java. In fact, the company at which all of this took place was having intermittent trouble with its JSP server pages. Than trouble was, eventually, tracked, exactly, to the fact that the pages were guarded with double-check locks. For most of the Oughts, writing a double-checked lock could get you fired.

A new and corrected version of the double-checked lock has become popular since then. In this new version, the variable that is checked twice is a Java `volatile`. This change makes the new idiom correct.

I was reading some Dagger code, the other day and was amused to notice that Dagger actually has a small, generic DCL wrapper class that it uses to lazily create objects that must be singletons, even in a concurrent environment. It got me to thinking.

## The Question

Here is a very simple double-checked lock, similar to the one that Dagger uses:

```Java
class DoubleCheckedProvider<A>(private val provider: Provider<A>) : Provider<A> {
    private val lock = Object()
    @Volatile private var value: A? = null

    override fun get(): A {
        if (value == null) {
            synchronized(lock) {
                if (value == null) {
                    value = provider.get()
                }
            }
        }
        return value!!
    }
}
```

The idiom first checks the protected variable without synchronization. If that check produces a non-null result the variable has been initialized and can be returned immediately. If the value is null, the code synchronizes, to make sure the update is visible and atomic, assigns the new value and returns it. The idea is that, in many cases, this will allow the code to skip the costly synchronization.

Although making the variable `value` volatile definitely makes this code correct, it is interesting to wonder how it affects idiom performance. The assumption that the code makes is that accessing a volatile variable is significantly faster than synchronizing access to the same variable. Is that true?

On a JVM, accessing a volatile variable and synchronizing access to an analogous variable are somewhat similar operations. Both forms of access require a **memory-barrier**: an operation that guarantees that, regardless of registers, caches and cores, all accesses appear to refer to a single piece of state. If one thread writes a value to the volatile variable, then that is the value that all other threads see when reading the variable, until some other thread writes something else there.

Note that this is absolutely *not* the case for unsynchronized access to a non-volatile variable. If some thread writes a value to a variable that is not volatile, without synchronization, it is entirely possible that that value will never be seen by other threads reading from the same variable, even if the other threads *do* synchronize their accesses and if no other writes take place before they read. This is why the original double-checked lock idiom is broken.

Clearly, because they must synchronize a single piece of state through all of its multiple representations in various caches, memory barriers are moderately expensive things.

In addition, the target platform of interest is Android, not a JVM. The Android runtime has gone through several revisions \[see **Levin**: [Dalvik and ART](http://newandroidbook.com/files/Andevcon-ART.pdf) \]. The goals for the platform during those revisions have been significantly different from those of the JVM.

So, the question arises: Does the double check locking idiom actually optimize access to protected state?

## The Experiment

The experiment described below attempts to benchmark access times for three types of locks and a reference null lock. In addition to the simply synchronized and double-checked locks, I have also included a spin-lock. This latter lock is used in RxJava, for similar purposes.

For each type of lock the experiment tests the time to access the protected variable in each of the two possible states:

* it is already initialized and can simply return its current state 
* the internal state must to be updated before it can be returned. 

For the rest of this article I will abbreviated these two states: read, compare and return, and read, compare, write and return, as, simply, “read” and “write”

For each of these two cases the experiment tests the access time when the lock is uncontested (that is, there is only one thread attempting to access the lock) and also under various levels of contention (that is, other threads are attempting to access the same lock at the same time).

I ran the entire test suite on three different devices. I would be interested in running similar tests on other devices, especially a couple of the popular Samsung phones.

Benchmarks are notoriously difficult to write and analyze. I’ve tried to be careful to assure that I have tested what I intended to test. None the less, I, absolutely, invite others to re-examine the experiment. The code is available here: [here](https://github.com/bmeike/FastLock). I would be delighted to have others reproduce or amend my results.

## Method

This experiment is meant to compare the various locks in an environment that is as like as possible to that in which they would be used in an actual application.

It is a simple Android application. Its main Activity operates in two modes, either running the test suite once, or running it continuously with a 1 minute delay between runs. I used the first mode only to refine and debug the application until I was confident that it was performing as intended.

I ran the test on three devices:

  Device  | Version | CPU | Notes 
----------|:-------:|:-----------------:|:-------------------------|
Nexus 4 | 5.1.1 | Snapdragon S4 Pro | Test phone: minimal applications
Nexus 6 | 7.0 | Snapdragon 805 | Personal phone: many installed apps and, presumably, multiple active periodic processes
Pixel 2 | 8.1.0 | Snapdragon 835 | Test phone: minimal applications

All devices were plugged in to power for the duration of the experiment. All were set to stay awake while attached to power and all were in airplane mode.

The results presented below are from approximately 24 hours of running the application, uninterrupted, on each device. On each device I installed the application immediately before starting the experiment in the hope of seeing the effects of ART optimization. The test application remained in foreground throughout the 24 hour test period. 

Each run of the test suite produces a single average time for 300000 accesses to each of the two different lock states (read and write), for each of the four types of lock. The four records are, asynchronously, stored in a SQLite database.

I chose 300000 simply because it causes the run of a single iteration to take a few seconds on the slowest of the test devices. This number of accesses is constant, regardless of the number of threads used in the experiment.

For each run of the suite, the harness makes a random choice for the number of threads that will contend for the lock: 1, 5, 10, or 20. In order to maximize contention, all threads wait at a `CyclicBarrier` at the start of the run. Test threads must also synchronize to record the timing data for each iteration of the test.

The harness also randomizes the order in which the various types of locks are tested, in order to minimize any bias that might be caused by always running one of the tests immediately before or after some other test.

A single run of the test produces one data record for each lock type. The record includes the id of the lock mechanism under test, the time at which the test ran, the number of threads in contention, and the two average times, one the read average and the other the write average.

A single test for a given lock tests first the access time for a read access and then a write access, always in that order. The value returned by the test method is saved, without synchronization, into a public harness field. I intend this completely thread-unsafe store to prevent any confounding optimizations.

Testing write access requires a new object for each test. This implies that the timing data may be distorted by occasional garbage collections.

### Null Test

The null test always returns the value with which it is initialized, ignoring update requests. Since "read" and "write" operations are identical I expect it take the same time to execute, regardless of how it is accessed. I expect neither the number of threads nor read verses write access to affect its timing.

### Synchronized Test

The synchronized test is the simplest possible functional lock. It always synchronizes, updates if necessary, and then returns the current state value. I expect this test to produce very similar times for read and write access. I also expect the times for both read and write access to degrade rapidly as the number of threads increases.

### Double Check Test

The double check test is the test that inspired the entire experiment. It determines the necessity of an update with a compare to a volatile value. It synchronizes only if an update is necessary. I expect this lock to perform about the same as the Synchronized Test on writes but much better on reads.

The code I use for the DCL in this experiment is not the exact code that Dagger uses. It is not even the example I showed above. Here are the essentials of the test code:

```Java
class DoubleCheckTest<T>(initVal: T) : Test<T>() {
    private val lock = Object()
    @Volatile
    private var value: T = initVal

    override fun get(newVal: T): T {
        val v1 = value
        if (v1 === newVal) {
        	return v1
        }
        
        synchronized(lock) {
            val v2 = value
            if (v1 === newVal) {
                return v2
            }
            value = newVal
            return value
        }
    }
}
```

The assignments to the temporary variables `v1` and `v2` are necessary to prevent Kotlin from complaining (correctly!) that value of the variable `newVal` might change during the comparison.

The important alteration is that, in order to test both the read and write cases with the same code, the the check comparison is against the method parameter, not a static `null`.

While I believe this code to be an accurate test of the double check idiom, it will *not*, as written, guarantee any kind agreement between threads about the value of the protected state.

### Spin Test

The spin test algorithm is used in several frameworks, most notably in this context, as mentioned earlier, RxJava (e.g., see [`ScheduledRunnable`](https://github.com/ReactiveX/RxJava/blob/fd63c492c576a2784a5665925457b75355ecffaa/src/main/java/io/reactivex/internal/schedulers/ScheduledRunnable.java#L78)). It uses neither volatiles nor explicit synchronization. Instead, it uses Java’s `AtomicReference`.

It first obtains the value held by the reference (the protected state) and checks to see if it needs updating. The internal workings of the `AtomicReference`, whether they be synchronization, volatiles, or something else entirely, are completely hidden.

If the value in the reference must be updated the code uses the Reference method `testAndSet`, which updates the reference only if its current value is the expected value. For this expected value, the code uses the value that it just obtained from the Reference. In other words, the update happens only if the reference’s value hasn’t changed since it was tested. If the value has changed the spin lock loops back to again fetch and test the value (thus the name “spin lock”).

Again, the test code is not exactly the code that would be used (as Dagger does) to create a lazy singleton.  The code for the analog to the `DoubleCheckedProvider`, shown at the beginning of this article, would look like this:

```Java
class SpinProvider<A>(private val provider: Provider<A>) : Provider<A> {
    private val value: AtomicReference<A> = AtomicReference()

    override fun get(): A {
        while (true) {
            val v1 = value.get()
            if (v1 != null) {
                return v1
            }
            val v2 = provider.get()
            if (value.compareAndSet(null, v2)) {
                return v2
            }
        }
    }
}
```

I do not have clear predictions for the behavior of this lock. One might reasonably expect it to perform somewhat worse than the DCL on read but as well or better on write. It seems probably that it will be affected adversely by contention.

## Results

After running the experiment I pulled the application databases from each device, converted them to CSV format, and used [`gnuplot`](http://www.gnuplot.info/) to plot graphs. In a couple of cases I removed a small number of outlying points from the data. Also, on a couple of the graphs, in order to reveal some tightly grouped values, I have set the y-range so that a few points are off the top of the graph.

In the first set of four test graphs below read test points are represented by “+”s and write test points by “x”s. The uncontested tests are purple. Points for contested tests with 5, 10, and 20 threads are drawn in blue, green, and red, respectively.

### Null Test

![null test](https://portabledroid.files.wordpress.com/2018/08/null4.jpg)

These graphs set the error bars for the other experiments. Since the read and write states for the Null test are exactly the same, any perceived differences are pure error.

For an uncontested lock, the errors are very small. Oddly — and contrary to initial expectations — both the average time to access and the amount of error increase as the number of threads in contention for the lock goes up.

### Synchronized Test

![sync test](https://portabledroid.files.wordpress.com/2018/08/sync4.jpg)

As predicted, simple synchronization degrades rapidly under contention. Against expectations, the times for read and write access spread considerably, as contention rises. Quite surprising, indeed, is that, on the two Nexus devices, it is *reading*, not writing, that degrades fastest.

### Double Check Test

![double-check test](https://portabledroid.files.wordpress.com/2018/08/2check4.jpg)

This test looks very much as expected. Under contention, write time degrades significantly. Read times degrade much more slowly. On two of the devices, they stay nearly constant.

### Spin Test

![spin test](https://portabledroid.files.wordpress.com/2018/08/spin4.jpg)

The spin idiom, again, degrades under contention. On all except the Pixel 2, however the degradation for read and write access are nearly identical. Even on the Pixel, the degradation for the two different modes is very similar.

In the next set of two comparison graphs, read test points are, again, represented by “+”s and write test points by “x”s. Points for the test types: null, synchronized, double check and spin, are drawn in purple, blue, green, and red, respectively. In this section I present only the graphs for the two extremes, 1 and 20 threads.

### Uncontested Access

![1 thread test](https://portabledroid.files.wordpress.com/2018/08/uncontested4.jpg)

In an uncontested environment the behaviors of the three locking mechanisms differ very little. The times for read access for two optimized algorithms are somewhat better than the times for simple synchronization. The write times for the optimized algorithms, however, appear to be somewhat worse than simple synchronization.

### Contested Access: 20 threads

![20 thread test](https://portabledroid.files.wordpress.com/2018/08/contested4.jpg)

In a contested access environment, reading any of the optimized locks is faster than reading the simple synchronized lock. The most apparent feature of this test is that writing a double checked lock is very slow: substantially slower than a write with simple synchronization.

## Analysis

Nearly as expected, the behaviors of each of the tests degrades as contention increases.

The results for read and write Null tests — actually identical reads of a final variable — are reasonably consistent. An examination of the byte code provides an explanation for the consistency with which the read times are slower than the write times. The test harness code that runs a test looks like this:

```Java
fun runOnce() {
    val newVal = Object()
    val tRead = measureNanoTime { value = readTest.get(initVal) }
    val tWrite = measureNanoTime { value = writeTest.get(newVal) }
    update(tRead, tWrite)
}
```

An examination of the corresponding byte code provides some insight. Although the reference to `newVal` is already in a register, the reference to `initVal` has to be loaded from a field. The two branches are not identical.

The read test looks like this:

```
iget-object v6, p0, Llocktest/TestOne;
	->readTest:Llocktest/Test;
iget-object v7, p0, Llocktest/TestOne;
	->initVal:Ljava/lang/Object;
invoke-virtual {v6, v7}, Llocktest/Test;
	->get(Ljava/lang/Object;)Ljava/lang/Object;
```

whereas the write test looks like this:

```
iget-object v7, p0, Llocktest/TestOne;
	->writeTest:Llocktest/Test;
invoke-virtual {v7, v0}, Llocktest/Test;
	->get(Ljava/lang/Object;)Ljava/lang/Object;
```

I am less able to explain the degradation of the Null test as contention increases. I originally suspected a hidden synchronization, perhaps in `getElapsedTimeNanos`. Reading the code for that function, however, I can find no such synchronization. Instead, perhaps the degradation is an artifact of scheduling: there are more threads than there are cores on any of the devices.

The baseline standard deviations for all Null write tests, for the three devices are 5.99ms, 17.70ms, and 1.09ms, respectively, for the Nexus 4, the Nexus 6 and Pixel 2.

It is interesting to observe the rate of degradation of the Null test, across devices. On the older, Nexus devices the degradation is nearly exponential. On the Pixel, however, it appears to be nearly logarithmic. It would be interesting to conduct further tests in this area.

The behavior of the simple synchronization test is the most surprising result in the entire experiment. I admit to revisiting the code, both in the application and in the analysis tools, several times, to be sure that I had not reversed the results.

Because the behavior does not appear in other tests, I not am inclined not to suspect the harness. Examining the byte code for the test does not seem to provide any useful insights either:

```
.method public get(Ljava/lang/Object;)Ljava/lang/Object;
	 iget-object v0, p0, Llocktest/SynchronizedTest;
		->lock:Ljava/lang/Object;

	monitor-enter v0
	const/4 v1, 0x0

	.local v1, "$i$a$1$synchronized":I
  :try_start_0
	iget-object v2, p0, Llocktest/SynchronizedTest;
		->value:Ljava/lang/Object;
	if-eq v2, p1, :cond_0
	iput-object p1, p0, Llocktest/SynchronizedTest;
		->value:Ljava/lang/Object;
  :try_end_0
	.catchall {:try_start_0 .. :try_end_0} :catchall_0

  :cond_0
	nop
	.end local v1    # "$i$a$1$synchronized":I
	monitor-exit v0

	return-object p1
	// …
.end method
```

For the single case of a simple synchronization lock on the Nexus 4, I created an alternative version of the experiment (branch: `rand-test`) that randomly re-orders the read and write tests, and moves the initialization value to a local variable (register) in the harness. The results from running this test are different — times are closer together — but not dramatically so.

Note that the Pixel’s behavior on the simple synchronization test is dramatically better and much more what I expected: write time degrades as the lock is contested. This might be because the Pixel is the most modern of the three devices and has twice the number of cores. Alternatively, perhaps there has been a significant improvement in either ART’s or the CPU’s handling of memory barriers.

The double check algorithm performs very much as billed. Both read and write access times degrade as lock contention increases but, unlike the simple synchronization test, read access degrades much more slowly than write access.

Note that the times for write case of the double check idiom are not the same as the write times for the simple synchronization test. This is a bit surprising, since the source code for the double-check write case is, almost identical to the source code for simple synchronization.

The byte code for the double-check idiom, however, is significantly different from the simple synchronization bytecode. The label `cond-1` marks the beginning of the write branch. It contains a second read from the protected variable:

```
.method public get(Ljava/lang/Object;)Ljava/lang/Object;
	iget-object v0, p0, Llocktest/DoubleCheckTest;
		->value:Ljava/lang/Object;
	.local v0, "v1":Ljava/lang/Object;
	if-ne v0, p1, :cond_0
	return-object v0

  :cond_0
	iget-object v1, p0, Llocktest/DoubleCheckTest;
		->lock:Ljava/lang/Object;
	monitor-enter v1
	const/4 v2, 0x0
	.local v2, "$i$a$1$synchronized":I

  :try_start_0
	iget-object v3, p0, Llocktest/DoubleCheckTest;
		->value:Ljava/lang/Object;
  :try_end_0
	.catchall {:try_start_0 .. :try_end_0} :catchall_0

	.local v3, "v2":Ljava/lang/Object;
	if-ne v0, p1, :cond_1
	nop

	.end local v2    # "$i$a$1$synchronized":I
	.end local v3    # "v2":Ljava/lang/Object;
	monitor-exit v1

	return-object v3

	.restart local v2    # "$i$a$1$synchronized":I
	.restart local v3    # "v2":Ljava/lang/Object;

  :cond_1
  :try_start_1
	iput-object p1, p0, Llocktest/DoubleCheckTest;
		->value:Ljava/lang/Object;
	iget-object v2, p0, Llocktest/DoubleCheckTest;
		->value:Ljava/lang/Object;
  :try_end_1
	.catchall {:try_start_1 .. :try_end_1} :catchall_0

	.end local v2    # "$i$a$1$synchronized":I
	.end local v3    # "v2":Ljava/lang/Object;
	monitor-exit v1

	return-object v2
	// ...
.end method
```

Access times for the Spin algorithm degrade, nearly identically for both read and write access. But wait! Perhaps this isn’t a bad thing! Let’s compare the algorithms head to head.

### Uncontested Read

![uncontested read](https://portabledroid.files.wordpress.com/2018/08/uncontestedread4.jpg)

While both optimized algorithms outperform simple synchronization on an uncontested read, the improvement is barely significant: no more than a millisecond. The difference is well within the error-bars.

### Uncontested Write

![uncontested write](https://portabledroid.files.wordpress.com/2018/08/uncontestedwrite4.jpg)

The unexpected speed of a synchronized write make the results of this comparison surprising. In this environment, simple synchronization always beats double-check locking; on some devices by several milliseconds.

The spin lock is comparable with the double-check lock, beating it only on the Nexus 4. Even there, though, it does not out-perform simple synchronization.

Note that, as in the uncontested read test, the differences between the tests scores do not exceed the null test error.

### Contested Read

![contested read](https://portabledroid.files.wordpress.com/2018/08/contestedread4.jpg)

In a contested read environment the optimized algorithms clearly demonstrate their advantage. Times for the two optimized algorithms are within the margin of error of each other and both are significantly better (&gt; 100ms on two of the three devices) than simple synchronization.

### Contested Write

![contested write](https://portabledroid.files.wordpress.com/2018/08/contestedwrite4.jpg)

In this experiment, in a contested write environment, the double-check lock is the worst performer. It is never better than simple synchronization; on the two Nexus devices it is significantly worse. The spin lock outperforms not only the DCL, but also simple synchronization

## Conclusions

This experiment does not demonstrate ART optimization. This is probably because even the experimental Activity's `onPause` was never called during data collection. It would be interesting to stop the app, wait a while, and then run it again. It would also be interesting to have a look at its `.oat` file.

The answer to the question posed at the beginning of this experiment: "Is the DCL performant" is mixed. Regardless of the device or the Android version, the DCL definitely does provides significant optimization for contested reads.

In other environments, it does not fare so well. In this experiment, in an uncontested environment, it did not significantly outperform simple synchronization. In this experiment, in a contested write environment, it performed very poorly: worse than simple synchronization on some devices/Android pairs.

The big winner here, is the spin lock. In nearly every test it performs as at least as well as the DCL and in most, much better. Especially in a contested write environment, it outperformed the DCL by a substantial margin. 

## Summary

I undertook this experiment because I was curious. I hope it will stimulate discussion. Regardless of how that discussion goes, I think I have demonstrated the following:

* If you are going to attempt to optimize concurrent code, be very careful. Even if there are flaws in this experiment, the timings are absolutely real. If the experiment is flawed, then your code may be flawed and non-performant as well.
 
* If a lock is not contested, even on Android, optimizing it may, simply, not be important. A simple synchronized block may be entirely sufficient.

* If access to the lock is contested, it may well be worth optimizing access. In this case a double-checked lock may be an effective optimization
 
* When optimizing a lock, consider a spin lock. Especially if the protected state must be updated frequently, spin locks seems to be quite performant.

Again, the code, data and tools are [here](https://github.com/bmeike/FastLock)
