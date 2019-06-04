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
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Availability;
import io.rsocket.exceptions.MissingLeaseException;
import io.rsocket.frame.LeaseFrameFlyweight;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

public class LeaseHandler {
  private final MonoProcessor<Void> onClose = MonoProcessor.create();

  private final LeaseManager requesterLeaseManager;
  private final LeaseManager responderLeaseManager;
  private Consumer<ByteBuf> leaseFrameSender;
  private Requester requesterLeaseHandler;
  private Responder responderLeaseHandler;
  private ByteBufAllocator byteBufAllocator;
  private RSocketLeaseSender leaseSupport;

  public LeaseHandler(String rSocketSide, ByteBufAllocator byteBufAllocator) {
    Objects.requireNonNull(rSocketSide);
    this.byteBufAllocator = Objects.requireNonNull(byteBufAllocator);
    this.requesterLeaseManager = new LeaseManager(rSocketSide + " requester");
    this.responderLeaseManager = new LeaseManager(rSocketSide + " responder");
    this.requesterLeaseHandler = new Requester();
    this.responderLeaseHandler = new Responder();
    this.leaseSupport = new RSocketLeaseSender();
  }

  public Requester requester() {
    return requesterLeaseHandler;
  }

  public Responder responder() {
    return responderLeaseHandler;
  }

  public LeaseSender leaseSupport() {
    return leaseSupport;
  }

  public class Requester implements Availability {

    /** returns null on successful Lease use, exception otherwise */
    public MissingLeaseException useLease() {
      return LeaseHandler.useLease(requesterLeaseManager);
    }

    public void onReceiveLease(
        int timeToLiveMillis, int numberOfRequests, @Nullable ByteBuf metadata) {
      if (timeToLiveMillis > 0 && numberOfRequests > 0) {
        requesterLeaseManager.updateLease(timeToLiveMillis, numberOfRequests, metadata);
      }
    }

    @Override
    public double availability() {
      return requesterLeaseManager.availability();
    }
  }

  public class Responder implements Availability, Disposable {

    public MissingLeaseException useLease() {
      return LeaseHandler.useLease(responderLeaseManager);
    }

    @Override
    public double availability() {
      return responderLeaseManager.availability();
    }

    /*todo Leases should be received by Requester only: this exists
     * to workaround the way ClientServerInputMultiplexer routes frames*/
    public void onReceiveLease(
        int timeToLiveMillis, int numberOfRequests, @Nullable ByteBuf metadata) {
      if (timeToLiveMillis > 0 && numberOfRequests > 0) {
        requesterLeaseManager.updateLease(timeToLiveMillis, numberOfRequests, metadata);
      }
    }

    public void onSendLease(Consumer<ByteBuf> leaseFrameSender) {
      LeaseHandler.this.leaseFrameSender = leaseFrameSender;
    }

    @Override
    public void dispose() {
      onClose.onComplete();
    }

    @Override
    public boolean isDisposed() {
      return onClose.isTerminated();
    }
  }

  private class RSocketLeaseSender implements LeaseSender {

    @Override
    public void sendLease(int timeToLiveMillis, int numberOfRequests, @Nullable ByteBuf metadata) {
      if (!isDisposed()) {
        ByteBuf leaseFrame =
            LeaseFrameFlyweight.encode(
                byteBufAllocator, timeToLiveMillis, numberOfRequests, metadata);
        leaseFrameSender.accept(leaseFrame);
        responderLeaseManager.updateLease(timeToLiveMillis, numberOfRequests, metadata);
      }
    }

    @Override
    public Lease currentLease() {
      return responderLeaseManager.getLease();
    }

    @Override
    public Mono<Void> onClose() {
      return onClose;
    }

    @Override
    public boolean isDisposed() {
      return onClose.isTerminated();
    }
  }

  private static MissingLeaseException useLease(LeaseManager leaseManager) {
    Lease lease = leaseManager.useLease();
    if (lease == null) {
      return null;
    }
    return new MissingLeaseException(lease, leaseManager.getTag());
  }
}
