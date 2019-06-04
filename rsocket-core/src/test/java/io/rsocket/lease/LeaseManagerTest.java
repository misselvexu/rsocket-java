package io.rsocket.lease;

import static org.junit.Assert.*;

import io.netty.buffer.Unpooled;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

public class LeaseManagerTest {

  private LeaseManager leaseManager;

  @Before
  public void setUp() {
    leaseManager = new LeaseManager("test");
  }

  @Test
  public void initialLeaseAvailability() {
    assertEquals(0.0, leaseManager.availability(), 1e-5);
  }

  @Test
  public void useNoRequests() {
    Lease lease = leaseManager.useLease();
    assertNotNull(lease);
    assertTrue(lease.isEmpty());
  }

  @Test
  public void updateLeaseSetsAvailability() {
    leaseManager.updateLease(2, 100, Unpooled.EMPTY_BUFFER);
    assertEquals(1.0, leaseManager.availability(), 1e-5);
  }

  @Test
  public void useLeaseDecreasesAvailability() {
    leaseManager.updateLease(30_000, 2, Unpooled.EMPTY_BUFFER);
    Lease lease = leaseManager.useLease();
    assertEquals(0.5, leaseManager.availability(), 1e-5);
    assertNull(lease);
    lease = leaseManager.useLease();
    assertEquals(0.0, leaseManager.availability(), 1e-5);
    assertNull(lease);
    lease = leaseManager.useLease();
    assertNotNull(lease);
    assertFalse(lease.isValid());
    assertFalse(lease.isExpired());
    assertEquals(0, lease.getAllowedRequests());
  }

  @Test
  public void useTimeout() {
    int numberOfRequests = 1;
    leaseManager.updateLease(1, numberOfRequests, Unpooled.EMPTY_BUFFER);
    Mono.delay(Duration.ofMillis(100)).block();
    Lease lease = leaseManager.useLease();

    assertNotNull(lease);
    assertTrue(lease.isExpired());
    assertEquals(numberOfRequests, lease.getAllowedRequests());
    assertFalse(lease.isValid());
    assertFalse(lease.isEmpty());
  }
}
