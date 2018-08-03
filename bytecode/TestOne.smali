
.method public final runOnce()V
    new-instance v0, Ljava/lang/Object;
    invoke-direct {v0}, Ljava/lang/Object;-><init>()V
    .local v0, "newVal":Ljava/lang/Object;
    
    const/4 v1, 0x0

    move v2, v1
    .local v2, "$i$f$measureNanoTime":I
    invoke-static {}, Ljava/lang/System;->nanoTime()J
    move-result-wide v3

    .local v3, "start$iv":J
    move v5, v1
    .local v5, "$i$a$1$measureNanoTime":I

    iget-object v6, p0, Lnet/callmeike/android/fastlock/test/TestOne;->readTest:Lnet/callmeike/android/fastlock/test/Test;
    iget-object v7, p0, Lnet/callmeike/android/fastlock/test/TestOne;->initVal:Ljava/lang/Object;
    invoke-virtual {v6, v7}, Lnet/callmeike/android/fastlock/test/Test;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v6
    iput-object v6, p0, Lnet/callmeike/android/fastlock/test/TestOne;->value:Ljava/lang/Object;
    .end local v5    # "$i$a$1$measureNanoTime":I

    invoke-static {}, Ljava/lang/System;->nanoTime()J
    move-result-wide v5
    sub-long/2addr v5, v3

    .end local v2    # "$i$f$measureNanoTime":I
    .end local v3    # "start$iv":J
    move-wide v2, v5
    .local v2, "tRead":J
    move v4, v1

    .local v4, "$i$f$measureNanoTime":I
    invoke-static {}, Ljava/lang/System;->nanoTime()J
    move-result-wide v5

    .local v5, "start$iv":J
    nop
    .local v1, "$i$a$1$measureNanoTime":I
    
    iget-object v7, p0, Lnet/callmeike/android/fastlock/test/TestOne;->writeTest:Lnet/callmeike/android/fastlock/test/Test;
    invoke-virtual {v7, v0}, Lnet/callmeike/android/fastlock/test/Test;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v7
    iput-object v7, p0, Lnet/callmeike/android/fastlock/test/TestOne;->value:Ljava/lang/Object;
    .end local v1    # "$i$a$1$measureNanoTime":I
    
    invoke-static {}, Ljava/lang/System;->nanoTime()J
    move-result-wide v7
    sub-long/2addr v7, v5

    .end local v4    # "$i$f$measureNanoTime":I
    .end local v5    # "start$iv":J
    move-wide v4, v7
    .local v4, "tWrite":J
    invoke-direct {p0, v2, v3, v4, v5}, Lnet/callmeike/android/fastlock/test/TestOne;->update(JJ)V

    return-void
.end method
