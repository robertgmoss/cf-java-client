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

package org.cloudfoundry.spring.doppler;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.doppler.ContainerMetric;
import org.cloudfoundry.doppler.ContainerMetricsRequest;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Event;
import org.cloudfoundry.doppler.FirehoseRequest;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.cloudfoundry.doppler.StreamRequest;
import org.cloudfoundry.spring.client.SpringCloudFoundryClient;
import org.cloudfoundry.spring.util.network.ConnectionContext;
import org.cloudfoundry.spring.util.network.SslCertificateTruster;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.timer.Timer;
import reactor.io.buffer.Buffer;
import reactor.io.ipc.ChannelFlux;
import reactor.io.netty.config.ClientSocketOptions;
import reactor.io.netty.config.SslOptions;
import reactor.io.netty.http.HttpChannel;
import reactor.io.netty.http.HttpClient;
import reactor.io.netty.http.NettyHttpClient;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The Reactor-based implementation of {@link DopplerClient}
 */
@ToString
public final class ReactorDopplerClient implements DopplerClient {

    public static final String TOKEN = "bearer eyJhbGciOiJSUzI1NiJ9" +
        ".eyJqdGkiOiJlMjJjNzlmMC1lNzY5LTRkYzAtOGRhOS0yMjY1YzEyZDVmZDQiLCJzdWIiOiI0ZjI3ZDU2ZS0wYzcwLTRkZTctYTU1YS04MmYzYThmYzY2YzUiLCJzY29wZSI6WyJjbG91ZF9jb250cm9sbGVyLnJlYWQiLCJwYXNzd29yZC53cml0ZSIsImNsb3VkX2NvbnRyb2xsZXIud3JpdGUiLCJvcGVuaWQiLCJ1YWEudXNlciJdLCJjbGllbnRfaWQiOiJjZiIsImNpZCI6ImNmIiwiYXpwIjoiY2YiLCJncmFudF90eXBlIjoicGFzc3dvcmQiLCJ1c2VyX2lkIjoiNGYyN2Q1NmUtMGM3MC00ZGU3LWE1NWEtODJmM2E4ZmM2NmM1Iiwib3JpZ2luIjoidWFhIiwidXNlcl9uYW1lIjoiYmhhbGVAcGl2b3RhbC5pbyIsImVtYWlsIjoiYmhhbGVAcGl2b3RhbC5pbyIsInJldl9zaWciOiI3Y2Y4ZDZlYSIsImlhdCI6MTQ1NzczODAxNywiZXhwIjoxNDU3NzM4NjE3LCJpc3MiOiJodHRwczovL3VhYS5ydW4ucGl2b3RhbC5pby9vYXV0aC90b2tlbiIsInppZCI6InVhYSIsImF1ZCI6WyJjbG91ZF9jb250cm9sbGVyIiwicGFzc3dvcmQiLCJjZiIsInVhYSIsIm9wZW5pZCJdfQ.AJfW6SDZtZKtQH87ER6EuXYrY72xnsGWs2AH2XONjlnGErNKRYctvtG-nA5hvc9mH5U1M7BCIQ9jSjm7mZi9cvlsBk6fx6u3g-RQx4nyzQD0Zn1jjOAQAcE3WvlfN30f4WUJ37e1LbBwADkIgOV38D5vHB2V2KL5It-YZ58anvQMIGsjTPwip_L8fJ5jkq1kR_7veX0GlK-qovsFpLDZ3pF29E_107j9MXR1xB-v2qeR7jbTXy3K1_RqWh6XCu2Svn2Gupzoig0mWjwXY9s4aL4n6J9o7DJT3XaEARetQTRF6DrdYNP1Dfr2QRkvLWtYk9ODR-b8nawDz2pqsm2MwA";

    private final HttpClient<Buffer, Buffer> httpClient;

    @Builder
    ReactorDopplerClient(@NonNull SpringCloudFoundryClient cloudFoundryClient) {
        this(getHttpClient(cloudFoundryClient.getConnectionContext()));
    }

    ReactorDopplerClient(HttpClient<Buffer, Buffer> httpClient) {
        this.httpClient = httpClient;
    }

    @SuppressWarnings("rawtypes")  // TODO: Remove
    @Override
    public Flux<ContainerMetric> containerMetrics(ContainerMetricsRequest request) {
        return this.httpClient
            .get("https://doppler.run.pivotal.io/apps/" + request.getApplicationId() + "/containermetrics")
            .log("stream.afterGet")
            .flatMap(ChannelFlux::input)
            .log("stream.afterInput")
            .map(Buffer::asString)
            .log("stream.afterString")
            .cast(ContainerMetric.class);
    }

    @Override
    public Flux<Event> firehose(FirehoseRequest request) {
        return null; // TODO
    }

    @Override
    public Flux<LogMessage> recentLogs(RecentLogsRequest request) {
        return null; // TODO
    }

    @Override
    public Flux<Event> stream(StreamRequest request) {
        return null; // TODO
    }

    private static HttpClient<Buffer, Buffer> getHttpClient(ConnectionContext connectionContext) {
        URI root = getRoot(connectionContext.getCloudFoundryClient(), connectionContext.getSslCertificateTruster());
        InetSocketAddress connectAddress = new InetSocketAddress(root.getHost(), root.getPort());

        return new NettyHttpClient(Timer.create(), connectAddress, new ClientSocketOptions(), new SslOptions())
            .preprocessor(new Function<HttpChannel<Buffer, Buffer>, ChannelFlux<Buffer, Object>>() {

                @Override
                public ChannelFlux<Buffer, Object> apply(HttpChannel<Buffer, Buffer> channel) {
                    return channel;
                }
            })
            ;
//            .preprocessor(channel -> connectionContext.getCloudFoundryClient()
//                .getAccessToken()
//                .map(accessToken -> channel.addHeader("Authorization", accessToken))
//                .get());
    }

    private static URI getRoot(CloudFoundryClient cloudFoundryClient, SslCertificateTruster sslCertificateTruster) {
        URI uri = requestInfo(cloudFoundryClient)
            .map(GetInfoResponse::getDopplerLoggingEndpoint)
            .map(URI::create)
            .get(Duration.ofSeconds(5));

        sslCertificateTruster.trust(uri.getHost(), uri.getPort(), 5, SECONDS);
        return uri;
    }

    private static Mono<GetInfoResponse> requestInfo(CloudFoundryClient cloudFoundryClient) {
        return cloudFoundryClient.info()
            .get(GetInfoRequest.builder()
                .build());
    }

}
