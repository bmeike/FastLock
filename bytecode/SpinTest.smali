
.field private final value:Ljava/util/concurrent/atomic/AtomicReference;
  // ...
.end field

# virtual methods
.method public get(Ljava/lang/Object;)Ljava/lang/Object;

  :goto_0
    nop

    iget-object v0, p0, Lnet/callmeike/android/fastlock/test/SpinTest;->value:Ljava/util/concurrent/atomic/AtomicReference;
    invoke-virtual {v0}, Ljava/util/concurrent/atomic/AtomicReference;->get()Ljava/lang/Object;
    move-result-object v0

    .local v0, "v1":Ljava/lang/Object;
    if-ne v0, p1, :cond_0
    return-object v0

    .line 76
  :cond_0
    iget-object v1, p0, Lnet/callmeike/android/fastlock/test/SpinTest;->value:Ljava/util/concurrent/atomic/AtomicReference;
    invoke-virtual {v1, v0, p1}, Ljava/util/concurrent/atomic/AtomicReference;->compareAndSet(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v1
    if-eqz v1, :cond_1
    return-object p1
    .end local v0    # "v1":Ljava/lang/Object;
    
  :cond_1
    goto :goto_0
.end method
