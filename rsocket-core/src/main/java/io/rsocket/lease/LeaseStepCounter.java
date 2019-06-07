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

public class LeaseStepCounter {
  private final int successStart;
  private final int rejectStart;
  private final long millisStart;
  private final int totalStart;

  private final int successEnd;
  private final int rejectEnd;
  private final long millisEnd;

  static LeaseStepCounter first(int successCount, int rejectCount, long startMillis) {
    return new LeaseStepCounter(0, 0, startMillis, 0, successCount, rejectCount, now());
  }

  private LeaseStepCounter(
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

  LeaseStepCounter next(int successCount, int rejectCount) {
    return new LeaseStepCounter(
        successEnd, rejectEnd, millisEnd, total(), successCount, rejectCount, now());
  }

  public int success(LeaseStepCounter from) {
    return successEnd - from.successStart;
  }

  public int success() {
    return success(this);
  }

  public int reject(LeaseStepCounter from) {
    return rejectEnd - from.rejectStart;
  }

  public int reject() {
    return reject(this);
  }

  public int total(LeaseStepCounter from) {
    int totalStart = from.rejectStart + from.successStart;
    int totalEnd = rejectEnd + successEnd;
    return totalEnd - totalStart;
  }

  public int total() {
    return total(this);
  }

  public long duration(LeaseStepCounter from) {
    return millisEnd - from.millisStart;
  }

  public long duration() {
    return duration(this);
  }

  public double rate(LeaseStepCounter from) {
    int total = total(from);
    int dif = total - totalStart;
    long interval = duration(from);
    return dif / (double) interval;
  }

  public double rate() {
    return rate(this);
  }

  private static long now() {
    return System.currentTimeMillis();
  }
}
