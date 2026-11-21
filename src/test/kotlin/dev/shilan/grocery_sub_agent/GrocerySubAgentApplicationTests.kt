package dev.shilan.grocery_sub_agent

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
