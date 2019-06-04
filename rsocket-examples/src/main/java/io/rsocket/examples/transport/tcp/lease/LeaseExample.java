package io.rsocket.examples.transport.tcp.lease;

import static java.time.Duration.ofSeconds;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import java.time.Duration;
import java.util.Date;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

public class LeaseExample {

  public static void main(String[] args) {

    LeaseSender serverLeaseSender = new LeaseSender();
    CloseableChannel server =
        RSocketFactory.receive()
            .lease(serverLeaseSender)
            .acceptor((setup, sendingRSocket) -> Mono.just(new ServerAcceptor(sendingRSocket)))
            .transport(TcpServerTransport.create("localhost", 7000))
            .start()
            .block();

    LeaseSender clientLeaseSender = new LeaseSender();
    RSocket clientRSocket =
        RSocketFactory.connect()
            .lease(clientLeaseSender)
            .acceptor(rSocket -> new ClientAcceptor())
            .transport(TcpClientTransport.create(server.address()))
            .start()
            .block();

    Flux.interval(ofSeconds(1))
        .flatMap(
            signal -> {
              System.out.println("Client requester availability: " + clientRSocket.availability());
              return clientRSocket
                  .requestResponse(DefaultPayload.create("Client request " + new Date()))
                  .doOnError(err -> System.out.println("Client request error: " + err))
                  .onErrorResume(err -> Mono.empty());
            })
        .subscribe(resp -> System.out.println("Client requester response: " + resp.getDataUtf8()));

    clientLeaseSender
        .leaseConnection()
        .flatMapMany(
            sender ->
                Flux.interval(ofSeconds(1), ofSeconds(10))
                    .map(tick -> sender)
                    .onBackpressureLatest())
        .subscribe(
            sender -> {
              System.out.println(
                  "Client responder sends new lease, current: " + sender.currentLease());
              sender.sendLease(3_000, 5);
            });

    serverLeaseSender
        .leaseConnection()
        .flatMapMany(
            sender ->
                Flux.interval(ofSeconds(1), ofSeconds(10))
                    .map(tick -> sender)
                    .onBackpressureLatest())
        .take(Duration.ofSeconds(120))
        .doFinally(s -> clientRSocket.dispose())
        .subscribe(
            sender -> {
              System.out.println(
                  "Server responder sends new lease, current: " + sender.currentLease());
              sender.sendLease(7_000, 5);
            });

    clientRSocket.onClose().block();
    server.dispose();
  }

  private static class LeaseSender implements Consumer<io.rsocket.lease.LeaseSender> {
    private final MonoProcessor<io.rsocket.lease.LeaseSender> leaseSupportMono =
        MonoProcessor.create();

    public Mono<io.rsocket.lease.LeaseSender> leaseConnection() {
      return leaseSupportMono;
    }

    @Override
    public void accept(io.rsocket.lease.LeaseSender leaseSender) {
      leaseSupportMono.onNext(leaseSender);
    }
  }

  private static class ClientAcceptor extends AbstractRSocket {
    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      return Mono.just(DefaultPayload.create("Client Response " + new Date()));
    }
  }

  private static class ServerAcceptor extends AbstractRSocket {
    private final RSocket senderRSocket;

    public ServerAcceptor(RSocket senderRSocket) {
      this.senderRSocket = senderRSocket;
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      System.out.println("Server requester availability: " + senderRSocket.availability());
      senderRSocket
          .requestResponse(DefaultPayload.create("Server request " + new Date()))
          .doOnError(err -> System.out.println("Server request error: " + err))
          .onErrorResume(err -> Mono.empty())
          .subscribe(
              resp -> System.out.println("Server requester response: " + resp.getDataUtf8()));

      return Mono.just(DefaultPayload.create("Server Response " + new Date()));
    }
  }
}
