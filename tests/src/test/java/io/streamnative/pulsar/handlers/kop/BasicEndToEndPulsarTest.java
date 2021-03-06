/**
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
package io.streamnative.pulsar.handlers.kop;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import lombok.Cleanup;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Producer;
import org.testng.annotations.Test;

/**
 * Basic end-to-end test with `entryFormat=pulsar`.
 */
public class BasicEndToEndPulsarTest extends BasicEndToEndTestBase {

    public BasicEndToEndPulsarTest() {
        super("pulsar");
    }

    @Test(timeOut = 20000)
    public void testNullValueMessages() throws Exception {
        final String topic = "test-produce-null-value";

        @Cleanup
        final KafkaProducer<String, String> kafkaProducer = newKafkaProducer();
        sendSingleMessages(kafkaProducer, topic, Arrays.asList(null, ""));
        sendBatchedMessages(kafkaProducer, topic, Arrays.asList("test", null, ""));

        @Cleanup
        final Producer<byte[]> pulsarProducer = newPulsarProducer(topic);
        sendSingleMessages(pulsarProducer, Arrays.asList(null, ""));
        sendBatchedMessages(kafkaProducer, topic, Arrays.asList("test", null, ""));

        final List<String> expectValues = Arrays.asList(null, "", "test", null, "", null, "", "test", null, "");

        // TODO: Currently there's a bug with MultiTopicsConsumerImpl that it cannot receive messages with null
        //  value. So here we just subscribe a single partition with ConsumerImpl.
        //  See https://github.com/apache/pulsar/pull/9113 for details.
        @Cleanup
        final Consumer<byte[]> pulsarConsumer = newPulsarConsumer(topic + "-partition-0");
        List<String> pulsarReceives = receiveMessages(pulsarConsumer, expectValues.size());
        assertEquals(pulsarReceives, expectValues);

        @Cleanup
        final KafkaConsumer<String, String> kafkaConsumer = newKafkaConsumer(topic);
        List<String> kafkaReceives = receiveMessages(kafkaConsumer, expectValues.size());
        assertEquals(kafkaReceives, expectValues);
    }
}
