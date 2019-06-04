package io.rsocket.transport;

import io.rsocket.AbstractRSocket;
import io.rsocket.Closeable;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RSocketFactoryLeaseIntegrationTest {

  @Test
  void serverRejectsUnsupportedLease() {
    CloseableChannel server =
        RSocketFactory.receive()
            .acceptor((setup, sendingSocket) -> Mono.just(new AbstractRSocket() {}))
            .transport(TcpServerTransport.create("localhost", 0))
            .start()
            .block();

    Collection<Throwable> errors = new ArrayList<>();
    RSocketFactory.connect()
        .errorConsumer(errors::add)
        .lease(leaseSender -> {})
        .transport(TcpClientTransport.create(server.address()))
        .start()
        .flatMap(Closeable::onClose)
        .doFinally(s -> server.dispose())
        .as(StepVerifier::create)
        .expectComplete()
        .verify(Duration.ofSeconds(5));
    Assertions.assertThat(errors).hasSize(1);
    String message = errors.iterator().next().getMessage();
    Assertions.assertThat(message).isEqualTo("lease is not supported");
  }
}
