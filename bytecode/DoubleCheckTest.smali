.field private volatile value:Ljava/lang/Object;
  // ...
.end field

.method public get(Ljava/lang/Object;)Ljava/lang/Object;
    iget-object v0, p0, Lnet/callmeike/android/fastlock/test/DoubleCheckTest;->value:Ljava/lang/Object;
    .local v0, "v1":Ljava/lang/Object;
    if-ne v0, p1, :cond_0
    return-object v0

  :cond_0
    iget-object v1, p0, Lnet/callmeike/android/fastlock/test/DoubleCheckTest;->lock:Ljava/lang/Object;

    monitor-enter v1
    const/4 v2, 0x0
    .local v2, "$i$a$1$synchronized":I
  :try_start_0
    iget-object v3, p0, Lnet/callmeike/android/fastlock/test/DoubleCheckTest;->value:Ljava/lang/Object;
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
    iput-object p1, p0, Lnet/callmeike/android/fastlock/test/DoubleCheckTest;->value:Ljava/lang/Object;
    iget-object v2, p0, Lnet/callmeike/android/fastlock/test/DoubleCheckTest;->value:Ljava/lang/Object;
  :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    .end local v2    # "$i$a$1$synchronized":I
    .end local v3    # "v2":Ljava/lang/Object;
    monitor-exit v1

    return-object v2

  :catchall_0
    move-exception v2
    
    monitor-exit v1

    throw v2
.end method
