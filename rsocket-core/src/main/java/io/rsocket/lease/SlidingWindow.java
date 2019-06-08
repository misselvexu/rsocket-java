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

public class SlidingWindow {
  private final int successStart;
  private final int rejectStart;
  private final long millisStart;
  private final int totalStart;

  private final int successEnd;
  private final int rejectEnd;
  private final long millisEnd;

  static SlidingWindow first(int successCount, int rejectCount, long startMillis) {
    return new SlidingWindow(0, 0, startMillis, 0, successCount, rejectCount, now());
  }

  private SlidingWindow(
      int successStart,
      int rejectStart,
      long millisStart,
      int totalStart,
      int successEnd,
      int rejectEnd,
      long millisEnd) {
    this.successStart = successStart;
    this.rejectStart = rejectStart;
    this.millisStart = millisStart;
    this.totalStart = totalStart;
    this.successEnd = successEnd;
    this.rejectEnd = rejectEnd;
    this.millisEnd = millisEnd;
  }

  SlidingWindow next(int successCount, int rejectCount) {
    return new SlidingWindow(
        successEnd, rejectEnd, millisEnd, total(), successCount, rejectCount, now());
  }

  public int success(SlidingWindow from) {
    return successEnd - from.successStart;
  }

  public int success() {
    return success(this);
  }

  public int reject(SlidingWindow from) {
    return rejectEnd - from.rejectStart;
  }

  public int reject() {
    return reject(this);
  }

  public int total(SlidingWindow from) {
    int totalStart = from.rejectStart + from.successStart;
    int totalEnd = rejectEnd + successEnd;
    return totalEnd - totalStart;
  }

  public int total() {
    return total(this);
  }

  public long duration(SlidingWindow from) {
    long start = from.millisStart;
    long end = this.millisEnd;
    long d = end - start;
    if (d < 0) {
      throw new IllegalStateException(String.format("ended before start: %d, %d", start, end));
    }
    return d;
  }

  public long duration() {
    return duration(this);
  }

  public double rate() {
    int total = total();
    int dif = total - totalStart;
    long interval = duration();
    if (interval == 0) {
      return dif < 0 ? Double.NEGATIVE_INFINITY : dif > 0 ? Double.POSITIVE_INFINITY : Double.NaN;
    }
    return dif / (double) interval;
  }

  private static long now() {
    return System.currentTimeMillis();
  }
}
