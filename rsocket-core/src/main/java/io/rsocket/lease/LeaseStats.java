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

/**
 * Represent statistics of responder lease represented as sliding window of step counters sampled at
 * given intervals
 */
public interface LeaseStats extends LeaseCounters {
  /** @return lease these stats are associated with */
  Lease lease();

  /**
   * @return snapshot of step counters for current lease. Step counters at their respective indexes
   *     are not expected to change
   */
  LeaseCounters countersSnapshot();

  @Override
  LeaseStepCounter counter(int counterIndex);

  @Override
  int countersSize();

  /** @return true if new Lease was sent already or RSocket terminated, false otherwise */
  boolean isDisposed();

  /** @return true if no leases were sent yet, false otherwise */
  boolean isEmpty();
}
