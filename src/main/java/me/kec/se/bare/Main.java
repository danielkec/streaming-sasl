
package me.kec.se.bare;

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.messaging.Channel;
import io.helidon.messaging.Emitter;
import io.helidon.messaging.Messaging;
import io.helidon.messaging.connectors.kafka.KafkaConfigBuilder;
import io.helidon.messaging.connectors.kafka.KafkaConnector;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public final class Main {

    public static void main(String[] args) {
        String token = System.getenv("OCI_AUTH_TOKEN");
        String ocid = "ocid1.streampool.oc1.phx.amaaaaaamevwycaap72ouurhfjrakuccakjpse5kenpkm5oikbgaadtq6byq";
        String username = "helidondev/daniel.kec@oracle.com/" + ocid;

        Config config = KafkaConnector.configBuilder()
                .bootstrapServers("cell-1.streaming.us-phoenix-1.oci.oraclecloud.com:9092")
                .groupId("example-group-1")
                .topic("TestStream")
                .property("sasl.mechanism", "PLAIN")
                .property("security.protocol", "SASL_SSL")
                .property("sasl.jaas.config",
                        "org.apache.kafka.common.security.plain.PlainLoginModule " +
                                "required " +
                                "username=\"" + username + "\" " +
                                "password=\"" + token + "\";")
                .autoOffsetReset(KafkaConfigBuilder.AutoOffsetReset.LATEST)
                .enableAutoCommit(true)
                .batchSize(1)
                .acks("0")
                .keyDeserializer(StringDeserializer.class)
                .valueDeserializer(StringDeserializer.class)
                .keySerializer(StringSerializer.class)
                .valueSerializer(StringSerializer.class)
                .build();

        Channel<String> fromStream = Channel.<String>builder()
                .name("from-stream")
                .publisherConfig(config)
                .build();

        Channel<String> toStream = Channel.<String>builder()
                .name("to-stream")
                .subscriberConfig(config)
                .build();

        // Prepare Kafka connector, can be used by any channel
        KafkaConnector kafkaConnector = KafkaConnector.create();

        if (args.length > 0) {
            Emitter<String> emitter = Emitter.create(toStream);

            Messaging messaging = Messaging.builder()
                    .connector(kafkaConnector)
                    .emitter(emitter)
                    .build()
                    .start();

            List<CompletionStage<Void>> futures = new ArrayList<>();
            for (String s : args) {
                System.out.println("Sending: " + s);
                futures.add(emitter.send(s));
            }
            Multi.create(futures).flatMap(cs -> Single.create(cs, true)).collectList().await();
            messaging.stop();
        } else {

            Messaging.builder()
                    .connector(kafkaConnector)
                    .subscriber(fromStream, multi -> multi.map(Message::getPayload)
                            .log()
                            .onError(Throwable::printStackTrace)
                            .forEach(p -> {
                                System.out.println("Received> " + p);
                            }))
                    .build()
                    .start();
            // Keep client up
            Single.never().await();
        }

    }

}
