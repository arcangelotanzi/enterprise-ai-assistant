package io.github.arcangelotanzi.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EnterpriseAiAssistantApplicationTests {

    @Test
    void contextLoads() {}
}
