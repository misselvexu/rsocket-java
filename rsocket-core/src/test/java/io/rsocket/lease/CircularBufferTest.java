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

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class CircularBufferTest {

  private RSocketLeaseStats.CircularBuffer<Integer> circularBuffer;

  @Before
  public void setUp() {
    circularBuffer = new RSocketLeaseStats.CircularBuffer<>(3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getNonExistentIndex() {
    circularBuffer.get(1);
  }

  @Test(expected = NullPointerException.class)
  public void offerNullElement() {
    circularBuffer.offer(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getNegativeIndex() {
    circularBuffer.get(-1);
  }

  @Test
  public void emptyBufferSize() {
    Assertions.assertThat(circularBuffer.size()).isEqualTo(0);
  }

  @Test
  public void singleElementBuffer() {
    circularBuffer = new RSocketLeaseStats.CircularBuffer<>(1);
    circularBuffer.offer(0).offer(1).offer(2);
    Assertions.assertThat(circularBuffer.get(0)).isEqualTo(2);
    Object[] snapshot = circularBuffer.snapshot();
    Assertions.assertThat(snapshot).hasSize(1).containsSequence(2);
  }

  @Test
  public void multielementBuffer() {
    circularBuffer.offer(0).offer(1).offer(2).offer(3).offer(4);
    Assertions.assertThat(circularBuffer.get(0)).isEqualTo(4);
    Assertions.assertThat(circularBuffer.get(1)).isEqualTo(3);
    Assertions.assertThat(circularBuffer.get(2)).isEqualTo(2);
    Object[] snapshot = circularBuffer.snapshot();
    Assertions.assertThat(snapshot).hasSize(3).containsSequence(4, 3, 2);
  }
}
