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

import static org.assertj.core.data.Percentage.withPercentage;

import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import reactor.core.publisher.Mono;

public class LeaseCountersTest {

  @Test
  public void firstRequest() {
    long startMillis = System.currentTimeMillis() - 5_000;
    LeaseStepCounter firstCounter = LeaseStepCounter.first(13, 17, startMillis);
    Assertions.assertThat(firstCounter.duration()).isCloseTo(5_000, withPercentage(1));
    Assertions.assertThat(firstCounter.reject()).isEqualTo(17);
    Assertions.assertThat(firstCounter.success()).isEqualTo(13);
    Assertions.assertThat(firstCounter.rate()).isCloseTo(30 / (float) 5_000, withPercentage(1));
  }

  @Test
  public void nextRequestGrowing() {
    long startMillis = System.currentTimeMillis();
    LeaseStepCounter firstCounter = LeaseStepCounter.first(13, 17, startMillis);

    long from = System.currentTimeMillis();
    Mono.delay(Duration.ofMillis(1000)).block();
    long to = System.currentTimeMillis();
    long actualDuration = to - from;

    LeaseStepCounter next = firstCounter.next(33, 37);
    Assertions.assertThat(next.duration()).isCloseTo(actualDuration, withPercentage(10));
    Assertions.assertThat(next.reject()).isEqualTo(20);
    Assertions.assertThat(next.success()).isEqualTo(20);
    Assertions.assertThat(next.rate()).isCloseTo(40 / (float) actualDuration, withPercentage(10));
  }

  @Test
  public void nextRequestSame() {
    long startMillis = System.currentTimeMillis();
    LeaseStepCounter firstCounter = LeaseStepCounter.first(13, 17, startMillis);

    long from = System.currentTimeMillis();
    Mono.delay(Duration.ofMillis(1000)).block();
    long to = System.currentTimeMillis();
    long actualDuration = to - from;

    LeaseStepCounter next = firstCounter.next(13, 17);
    Assertions.assertThat(next.duration()).isCloseTo(actualDuration, withPercentage(10));
    Assertions.assertThat(next.reject()).isEqualTo(0);
    Assertions.assertThat(next.success()).isEqualTo(0);
    Assertions.assertThat(next.rate()).isCloseTo(0, withPercentage(1));
  }

  @Test
  public void nextRequestDecay() {
    long startMillis = System.currentTimeMillis();
    LeaseStepCounter firstCounter = LeaseStepCounter.first(13, 17, startMillis);

    long from = System.currentTimeMillis();
    Mono.delay(Duration.ofMillis(1000)).block();
    long to = System.currentTimeMillis();
    long actualDuration = to - from;

    LeaseStepCounter next = firstCounter.next(20, 19);
    Assertions.assertThat(next.duration()).isCloseTo(actualDuration, withPercentage(10));
    Assertions.assertThat(next.reject()).isEqualTo(2);
    Assertions.assertThat(next.success()).isEqualTo(7);
    Assertions.assertThat(next.rate()).isCloseTo(-21 / (float) actualDuration, withPercentage(10));
  }
}
