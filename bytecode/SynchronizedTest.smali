
.field private value:Ljava/lang/Object;
  // ...
.end field


# virtual methods
.method public get(Ljava/lang/Object;)Ljava/lang/Object;
    iget-object v0, p0, Lnet/callmeike/android/fastlock/test/SynchronizedTest;->lock:Ljava/lang/Object;

    monitor-enter v0
    const/4 v1, 0x0

    .local v1, "$i$a$1$synchronized":I
  :try_start_0
    iget-object v2, p0, Lnet/callmeike/android/fastlock/test/SynchronizedTest;->value:Ljava/lang/Object;
    if-eq v2, p1, :cond_0
    iput-object p1, p0, Lnet/callmeike/android/fastlock/test/SynchronizedTest;->value:Ljava/lang/Object;
  :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

  :cond_0
    nop
    .end local v1    # "$i$a$1$synchronized":I
    monitor-exit v0

    return-object p1

  :catchall_0
    move-exception v1

    monitor-exit v0

    throw v1
.end method
