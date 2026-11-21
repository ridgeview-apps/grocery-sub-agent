package dev.shilan.grocery_sub_agent

import dev.shilan.grocery_sub_agent.agent.FakeChatModelConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(FakeChatModelConfig::class)
class GrocerySubAgentApplicationTests {

	@Test
	fun contextLoads() {
	}

}
