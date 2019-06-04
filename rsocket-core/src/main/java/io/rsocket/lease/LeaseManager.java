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

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class LeaseManager {
  private volatile LeaseImpl currentLease = LeaseImpl.empty();
  private final String tag;

  public LeaseManager(@Nonnull String tag) {
    this.tag = Objects.requireNonNull(tag, "tag");
  }

  public double availability() {
    LeaseImpl l = this.currentLease;
    return l.isValid() ? l.getAllowedRequests() / (double) l.getStartingAllowedRequests() : 0.0;
  }

  public void updateLease(int timeToLiveMillis, int numberOfRequests, @Nullable ByteBuf metadata) {
    currentLease.getMetadata().release();
    this.currentLease = LeaseImpl.create(timeToLiveMillis, numberOfRequests, metadata);
  }

  public Lease getLease() {
    return currentLease;
  }

  /** @return null if current Lease used successfully, Lease otherwise */
  @Nullable
  public Lease useLease() {
    LeaseImpl l = this.currentLease;
    boolean success = l.use(1);
    return success ? null : l;
  }

  public String getTag() {
    return tag;
  }

  @Override
  public String toString() {
    return "LeaseManager{" + "tag='" + tag + '\'' + '}';
  }
}
