package com.reportplatform.base.dapr;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.State;
import io.dapr.utils.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper around Dapr client providing retry logic with exponential backoff.
 * <p>
 * Retry policy: 3 attempts with backoff intervals of 1s, 5s, and 30s.
 * <p>
 * This wrapper provides a simplified API for common Dapr operations:
 * <ul>
 *     <li>Service invocation</li>
 *     <li>State store operations</li>
 *     <li>Pub/sub messaging</li>
 * </ul>
 */
public class DaprClientWrapper {

    private static final Logger log = LoggerFactory.getLogger(DaprClientWrapper.class);

    private static final int MAX_RETRIES = 3;
    private static final Duration FIRST_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

    private final DaprClient daprClient;
    private final Retry retrySpec;

    public DaprClientWrapper(DaprClient daprClient) {
        this.daprClient = Objects.requireNonNull(daprClient, "daprClient must not be null");
        this.retrySpec = Retry.backoff(MAX_RETRIES, FIRST_BACKOFF)
                .maxBackoff(MAX_BACKOFF)
                .jitter(0.1)
                .doBeforeRetry(signal -> log.warn(
                        "Dapr operation retry attempt {}/{} after error: {}",
                        signal.totalRetries() + 1,
                        MAX_RETRIES,
                        signal.failure().getMessage()
                ))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    throw new DaprOperationException(
                            "Dapr operation failed after " + MAX_RETRIES + " retries",
                            retrySignal.failure()
                    );
                });
    }

    /**
     * Invokes a method on another service via Dapr service invocation.
     *
     * @param appId      the Dapr app ID of the target service
     * @param methodName the method/endpoint to invoke
     * @param data       the request body
     * @param httpMethod the HTTP method to use
     * @param responseType the expected response type
     * @param <T>        response type
     * @return the response wrapped in Mono
     */
    public <T> Mono<T> invokeMethod(String appId, String methodName, Object data,
                                     HttpExtension httpMethod, TypeRef<T> responseType) {
        return daprClient.invokeMethod(appId, methodName, data, httpMethod, responseType)
                .retryWhen(retrySpec)
                .doOnError(e -> log.error("Failed to invoke method {}/{}: {}",
                        appId, methodName, e.getMessage()));
    }

    /**
     * Invokes a method on another service with no request body.
     */
    public <T> Mono<T> invokeMethod(String appId, String methodName,
                                     HttpExtension httpMethod, TypeRef<T> responseType) {
        return daprClient.invokeMethod(appId, methodName, null, httpMethod, responseType)
                .retryWhen(retrySpec)
                .doOnError(e -> log.error("Failed to invoke method {}/{}: {}",
                        appId, methodName, e.getMessage()));
    }

    /**
     * Saves state to a Dapr state store.
     *
     * @param storeName the name of the state store component
     * @param key       the state key
     * @param value     the state value
     * @return Mono completing when the state is saved
     */
    public Mono<Void> saveState(String storeName, String key, Object value) {
        return daprClient.saveState(storeName, key, value)
                .retryWhen(retrySpec)
                .doOnError(e -> log.error("Failed to save state {}/{}: {}",
                        storeName, key, e.getMessage()));
    }

    /**
     * Retrieves state from a Dapr state store.
     *
     * @param storeName the name of the state store component
     * @param key       the state key
     * @param type      the expected value type
     * @param <T>       value type
     * @return the state value wrapped in Mono
     */
    public <T> Mono<State<T>> getState(String storeName, String key, TypeRef<T> type) {
        return daprClient.getState(storeName, key, type)
                .retryWhen(retrySpec)
                .doOnError(e -> log.error("Failed to get state {}/{}: {}",
                        storeName, key, e.getMessage()));
    }

    /**
     * Deletes state from a Dapr state store.
     *
     * @param storeName the name of the state store component
     * @param key       the state key
     * @return Mono completing when the state is deleted
     */
    public Mono<Void> deleteState(String storeName, String key) {
        return daprClient.deleteState(storeName, key)
                .retryWhen(retrySpec)
                .doOnError(e -> log.error("Failed to delete state {}/{}: {}",
                        storeName, key, e.getMessage()));
    }

    /**
     * Publishes an event to a Dapr pub/sub topic.
     *
     * @param pubsubName the name of the pub/sub component
     * @param topicName  the topic to publish to
     * @param data       the event data
     * @return Mono completing when the event is published
     */
    public Mono<Void> publishEvent(String pubsubName, String topicName, Object data) {
        return daprClient.publishEvent(pubsubName, topicName, data)
                .retryWhen(retrySpec)
                .doOnError(e -> log.error("Failed to publish event to {}/{}: {}",
                        pubsubName, topicName, e.getMessage()));
    }

    /**
     * Publishes an event to a Dapr pub/sub topic with metadata.
     *
     * @param pubsubName the name of the pub/sub component
     * @param topicName  the topic to publish to
     * @param data       the event data
     * @param metadata   additional metadata for the message
     * @return Mono completing when the event is published
     */
    public Mono<Void> publishEvent(String pubsubName, String topicName, Object data,
                                    Map<String, String> metadata) {
        return daprClient.publishEvent(pubsubName, topicName, data, metadata)
                .retryWhen(retrySpec)
                .doOnError(e -> log.error("Failed to publish event to {}/{}: {}",
                        pubsubName, topicName, e.getMessage()));
    }

    /**
     * Returns the underlying Dapr client for advanced operations not covered by this wrapper.
     */
    public DaprClient unwrap() {
        return daprClient;
    }

    /**
     * Exception thrown when a Dapr operation fails after all retry attempts.
     */
    public static class DaprOperationException extends RuntimeException {
        public DaprOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
