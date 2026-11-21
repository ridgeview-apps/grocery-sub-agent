package dev.shilan.grocery_sub_agent.agent

import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/** Returns a fixed reply and records every prompt, so tests run without a model or GCP credentials. */
class FakeChatModel(private val reply: String) : ChatModel {
    val prompts = mutableListOf<Prompt>()

    override fun call(prompt: Prompt): ChatResponse {
        prompts += prompt
        return ChatResponse(listOf(Generation(AssistantMessage.builder().content(reply).build())))
    }
}

/** Stands in for Gemini in @SpringBootTest classes; the real model is switched off for unit tests in build.gradle.kts. */
@TestConfiguration
class FakeChatModelConfig {
    @Bean
    fun chatModel(): ChatModel = FakeChatModel("""{"productId": "P011", "reason": "Fake model reply"}""")
}
