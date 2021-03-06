/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.spring.logging;

import lombok.ToString;
import org.cloudfoundry.Validatable;
import org.cloudfoundry.logging.LogMessage;
import org.cloudfoundry.logging.StreamLogsRequest;
import org.cloudfoundry.util.ValidationUtils;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SchedulerGroup;
import reactor.core.subscriber.SubscriberWithContext;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@ToString
final class SpringStream {

    private final Logger logger = LoggerFactory.getLogger("cloudfoundry-client.request");

    private final ClientEndpointConfig clientEndpointConfig;

    private final URI root;

    private final SchedulerGroup schedulerGroup;

    private final WebSocketContainer webSocketContainer;

    SpringStream(WebSocketContainer webSocketContainer, ClientEndpointConfig clientEndpointConfig, URI root, SchedulerGroup schedulerGroup) {
        this.clientEndpointConfig = clientEndpointConfig;
        this.root = root;
        this.schedulerGroup = schedulerGroup;
        this.webSocketContainer = webSocketContainer;
    }

    public Flux<LogMessage> stream(StreamLogsRequest request) {
        return ws(request, builder -> builder.path("tail/").queryParam("app", request.getApplicationId()), LoggregatorMessageHandler::new);
    }

    private static String toString(String method, URI uri) {
        return String.format("%-6s %s", method, trimUri(uri));
    }

    private static URI trimUri(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
            .scheme(null).host(null).port(null)
            .build().encode().toUri();
    }

    @SuppressWarnings("unchecked")
    private <T, V extends Validatable> Flux<T> exchange(V request, Consumer<Subscriber<T>> exchange) {
        return ValidationUtils
            .validate(request)
            .flatMap(request1 -> Flux
                .generate((Long n, SubscriberWithContext<T, Void> subscriber) -> {
                    if (n != Long.MAX_VALUE) {
                        subscriber.onError(new IllegalArgumentException("Publisher doesn't support back pressure"));
                    }

                    exchange.accept(subscriber);
                })
            )
            .subscribeOn(this.schedulerGroup)
            .onBackpressureBuffer();
    }

    private <T> Flux<T> ws(Validatable request, Consumer<UriComponentsBuilder> builderCallback, Function<Subscriber<T>, MessageHandler> messageHandlerCreator) {
        AtomicReference<Session> session = new AtomicReference<>();

        return exchange(request, (Consumer<Subscriber<T>>) subscriber -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUri(this.root);
            builderCallback.accept(builder);
            URI uri = builder.build().toUri();

            MessageHandler messageHandler = messageHandlerCreator.apply(subscriber);
            ReactiveEndpoint<T> endpoint = new ReactiveEndpoint<>(messageHandler, subscriber);

            try {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug(SpringStream.toString("WS", uri));
                }

                session.set(this.webSocketContainer.connectToServer(endpoint, this.clientEndpointConfig, uri));
            } catch (DeploymentException | IOException e) {
                subscriber.onError(e);
            }
        }).doOnCancel(() -> {
            if (session.get() != null) {
                try {
                    session.get().close();
                } catch (IOException e) {
                    this.logger.warn("Failure closing session", e);
                }
            }
        });
    }


}
