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

public interface LeaseStatsWindows {

  /**
   * @param index index of sliding window from 0 to {@link #size() - 1}. 0 corresponds to most
   *     recent window
   * @return sliding window associated with given index
   */
  SlidingWindow window(int index);

  /** @return most recent sliding window of lease */
  default SlidingWindow window() {
    return window(0);
  }

  /** @return number of sliding windows for this Lease. */
  int size();
}
