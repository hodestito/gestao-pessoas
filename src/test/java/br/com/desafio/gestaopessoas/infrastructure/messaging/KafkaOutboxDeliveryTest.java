package br.com.desafio.gestaopessoas.infrastructure.messaging;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaOutboxDeliveryTest {
    @Test void unknownTypeCannotFallBackToAnUnrelatedTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        var delivery = new KafkaOutboxDelivery(kafka, "created", "status");
        assertThrows(IllegalArgumentException.class,
                () -> delivery.send("UnknownEmployeeEvent", UUID.randomUUID(), "{}"));
        verifyNoInteractions(kafka);
    }
}
