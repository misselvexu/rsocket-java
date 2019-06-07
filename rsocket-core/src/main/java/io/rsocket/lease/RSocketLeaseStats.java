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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;

class RSocketLeaseStats implements Disposable, LeaseStats {
  private static final RSocketLeaseStats EMPTY = new RSocketLeaseStats(LeaseImpl.empty(), 0, 0);

  private final AtomicBoolean disposed = new AtomicBoolean();
  private final Lease lease;
  private final Disposable nextStepDisposable;
  private final CircularBuffer<LeaseStepCounter> counters;
  private final long startMillis;

  public static RSocketLeaseStats create(Lease lease, long stepMillis, int stepCount) {
    return new RSocketLeaseStats(lease, stepMillis, stepCount);
  }

  public static RSocketLeaseStats empty() {
    return EMPTY;
  }

  private RSocketLeaseStats(Lease lease, long stepMillis, int stepCount) {
    this.lease = Objects.requireNonNull(lease);
    this.startMillis = System.currentTimeMillis();
    if (!lease.isEmpty() && stepMillis > 0 && stepCount > 0) {
      this.counters = new CircularBuffer<>(stepCount);
      this.nextStepDisposable =
          Flux.interval(Duration.ofMillis(stepMillis))
              .doOnCancel(() -> addNextStep())
              .doOnNext(ignored -> addNextStep())
              .subscribe();
    } else {
      this.nextStepDisposable = Disposables.disposed();
      this.counters = null;
    }
  }

  @Override
  public Lease lease() {
    return lease;
  }

  @Override
  public LeaseCounters countersSnapshot() {
    if (isEmpty()) {
      throw new IllegalArgumentException(
          "Stats are empty - should be checked with isEmpty() first.");
    }
    return new LeaseCounters() {
      private final Object[] leaseStepCounters = counters.snapshot();

      @Override
      public LeaseStepCounter counter(int counterIndex) {
        return (LeaseStepCounter) leaseStepCounters[counterIndex];
      }

      @Override
      public int countersSize() {
        return leaseStepCounters.length;
      }
    };
  }

  @Override
  public LeaseStepCounter counter(int counterIndex) {
    if (isEmpty()) {
      throw new IllegalArgumentException("Stats are empty");
    }
    return counters.get(counterIndex);
  }

  @Override
  public boolean isEmpty() {
    return counters == null || counters.size() == 0;
  }

  @Override
  public int countersSize() {
    CircularBuffer<LeaseStepCounter> c = this.counters;
    return c == null ? 0 : c.size();
  }

  @Override
  public void dispose() {
    if (disposed.compareAndSet(false, true)) {
      nextStepDisposable.dispose();
    }
  }

  @Override
  public boolean isDisposed() {
    return nextStepDisposable.isDisposed();
  }

  private void addNextStep() {
    Lease l = this.lease;
    boolean isEmpty = counters.size() == 0;
    LeaseStepCounter next =
        isEmpty
            ? LeaseStepCounter.first(
                l.getSuccessfulRequests(), l.getRejectedRequests(), startMillis)
            : counters.get(0).next(l.getSuccessfulRequests(), l.getRejectedRequests());
    counters.offer(next);
  }

  static class CircularBuffer<T> {
    private final Object[] arr;
    private int start;
    private int end;
    private int size;

    public CircularBuffer(int size) {
      this.arr = new Object[size];
    }

    public synchronized CircularBuffer<T> offer(T t) {
      Objects.requireNonNull(t, "element");
      if (end == start && size != 0) {
        start = moveIndex(start, 1);
      }
      size = Math.min(size + 1, arr.length);
      arr[end] = t;
      end = moveIndex(end, 1);
      return this;
    }

    public synchronized T get(int pos) {
      if (pos < 0) {
        throw new IllegalArgumentException("Position should be positive");
      }
      int size = this.size;
      if (pos >= size) {
        String msg = "Element does not exist: size: %d, position: %d";
        throw new IllegalArgumentException(String.format(msg, size, pos));
      }
      return (T) arr[moveIndex(start, size - 1 - pos)];
    }

    public synchronized Object[] snapshot() {
      int size = size();
      Object[] res = new Object[size];
      if (size > 0) {
        for (int i = 0; i < size; i++) {
          res[i] = get(i);
        }
      }
      return res;
    }

    public synchronized int size() {
      return size;
    }

    private int moveIndex(int idx, int offset) {
      return (idx + offset) % arr.length;
    }
  }
}
