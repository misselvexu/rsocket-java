package io.rsocket.lease;

import io.netty.buffer.ByteBufAllocator;
import java.util.function.Consumer;

public class LeaseSupport {
  private final LeaseHandler leaseHandler;
  private final LeaseHandler.Requester requesterLeaseHandler;
  private final LeaseHandler.Responder responderLeaseHandler;
  private final LeaseSender leaseSender;

  public LeaseSupport(boolean enabled, String side, ByteBufAllocator allocator) {
    leaseHandler = enabled ? new LeaseHandler(side, allocator) : null;
    requesterLeaseHandler = leaseHandler != null ? leaseHandler.requester() : null;
    responderLeaseHandler = leaseHandler != null ? leaseHandler.responder() : null;
    leaseSender = leaseHandler != null ? leaseHandler.leaseSupport() : null;
  }

  public LeaseHandler.Requester requesterHandler() {
    return requesterLeaseHandler;
  }

  public LeaseHandler.Responder responderHandler() {
    return responderLeaseHandler;
  }

  public void provideLeaseSender(Consumer<LeaseSender> leaseSupportConsumer) {
    if (leaseSender != null) {
      leaseSupportConsumer.accept(leaseSender);
    }
  }
}
