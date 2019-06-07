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
