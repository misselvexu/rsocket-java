/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.lease;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;

class RSocketLeaseSlidingWindowStats implements Disposable, LeaseSlidingWindowStats {
  private static final RSocketLeaseSlidingWindowStats EMPTY =
      new RSocketLeaseSlidingWindowStats(LeaseImpl.empty(), 0);

  private final AtomicBoolean disposed = new AtomicBoolean();
  private final Lease lease;
  private final long windowMillis;
  private final Disposable windowPollDisposable;

  private volatile int windowIndex = -1;
  private volatile Stats stats = Stats.start();

  public static RSocketLeaseSlidingWindowStats create(Lease lease, long rollingWindowMillis) {
    return new RSocketLeaseSlidingWindowStats(lease, rollingWindowMillis);
  }

  public static RSocketLeaseSlidingWindowStats empty() {
    return EMPTY;
  }

  private RSocketLeaseSlidingWindowStats(Lease lease, long rollingWindowMillis) {
    this.lease = lease;
    this.windowMillis = rollingWindowMillis;
    if (!lease.isEmpty()) {
      this.windowPollDisposable =
          Flux.interval(Duration.ofMillis(rollingWindowMillis))
              .subscribe(ignored -> calculateWindowStats(), err -> {}, this::calculateWindowStats);
    } else {
      this.windowPollDisposable = Disposables.disposed();
    }
  }

  @Override
  public Lease lease() {
    return lease;
  }

  @Override
  public boolean isEmpty() {
    return windowIndex == -1;
  }

  @Override
  public int totalRequests() {
    checkNotEmpty();

    Stats s = this.stats;
    int prevTotal = s.prevRejectedRequests() + s.prevSuccessfulRequests();
    int curTotal = s.curRejectedRequests() + s.curSuccessfulRequests();

    return curTotal - prevTotal;
  }

  @Override
  public int successfulRequests() {
    checkNotEmpty();
    Stats s = this.stats;
    return s.curSuccessfulRequests() - s.prevSuccessfulRequests();
  }

  @Override
  public int rejectedRequests() {
    checkNotEmpty();
    Stats s = this.stats;
    return s.curRejectedRequests() - s.prevRejectedRequests();
  }

  @Override
  public double totalRequestsRate() {
    checkNotEmpty();
    Stats s = this.stats;
    int prevTotal = s.prevRejectedRequests() + s.prevSuccessfulRequests();
    int curTotal = s.curRejectedRequests() + s.curSuccessfulRequests();
    int difTotal = curTotal - prevTotal;
    long interval = s.curWindowStartMillis() - s.prevWindowStartMillis();
    return difTotal / (double) interval;
  }

  @Override
  public int rollingWindowIndex() {
    return windowIndex;
  }

  @Override
  public long rollingWindowMillis() {
    return windowMillis;
  }

  @Override
  public void dispose() {
    if (disposed.compareAndSet(false, true)) {
      windowPollDisposable.dispose();
    }
  }

  @Override
  public boolean isDisposed() {
    return disposed.get();
  }

  private void calculateWindowStats() {
    /*never written concurrently*/
    stats = stats.nextWindow(lease);
    windowIndex++;
  }

  private void checkNotEmpty() {
    if (isEmpty()) {
      throw new IllegalStateException(
          "LeaseSlidingWindowStats are empty. Should be checked with LeaseSlidingWindowStats.isEmpty "
              + "before calling stats methods");
    }
  }

  private static class Stats {
    private final int prevSuccessfulRequests;
    private final int prevRejectedRequests;
    private final long prevWindowStartMillis;

    private final int curSuccessfulRequests;
    private final int curRejectedRequests;
    private final long curWindowStartMillis;

    public static Stats start() {
      return new Stats(0, 0, 0, 0, 0, now());
    }

    private Stats(
        int prevSuccessfulRequests,
        int prevRejectedRequests,
        long prevWindowStartMillis,
        int curSuccessfulRequests,
        int curRejectedRequests,
        long curWindowStartMillis) {
      this.prevSuccessfulRequests = prevSuccessfulRequests;
      this.prevRejectedRequests = prevRejectedRequests;
      this.prevWindowStartMillis = prevWindowStartMillis;
      this.curSuccessfulRequests = curSuccessfulRequests;
      this.curRejectedRequests = curRejectedRequests;
      this.curWindowStartMillis = curWindowStartMillis;
    }

    public Stats nextWindow(Lease lease) {
      return new Stats(
          curSuccessfulRequests,
          curRejectedRequests,
          prevWindowStartMillis,
          lease.getSuccessfulRequests(),
          lease.getRejectedRequests(),
          now());
    }

    public int prevSuccessfulRequests() {
      return prevSuccessfulRequests;
    }

    public int prevRejectedRequests() {
      return prevRejectedRequests;
    }

    public long prevWindowStartMillis() {
      return prevWindowStartMillis;
    }

    public int curSuccessfulRequests() {
      return curSuccessfulRequests;
    }

    public int curRejectedRequests() {
      return curRejectedRequests;
    }

    public long curWindowStartMillis() {
      return curWindowStartMillis;
    }

    private static long now() {
      return System.currentTimeMillis();
    }
  }
}
