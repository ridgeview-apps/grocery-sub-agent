package dev.shilan.grocery_sub_agent.agent

import dev.shilan.grocery_sub_agent.tools.OrderTools
import dev.shilan.grocery_sub_agent.tools.StockTools
import dev.shilan.grocery_sub_agent.tools.SubstitutionHistoryTools
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType

/** A fresh H2 database from schema.sql and data.sql with the real tools on top, without a Spring context. */
class TestDatabase : AutoCloseable {
    private val db = EmbeddedDatabaseBuilder()
        .generateUniqueName(true)
        .setType(EmbeddedDatabaseType.H2)
        .addScripts("schema.sql", "data.sql")
        .build()
    private val jdbc = JdbcClient.create(db)

    val orderTools = OrderTools(jdbc)
    val stockTools = StockTools(jdbc)
    val historyTools = SubstitutionHistoryTools(jdbc)

    fun agent(ranker: SubstituteRanker) = SubstitutionAgent(orderTools, stockTools, historyTools, ranker)

    fun pickerDecisions() = PickerDecisions(orderTools, stockTools, historyTools)

    override fun close() = db.shutdown()
}

/** A stand-in for the model: records every request, then answers it with `answer`. */
class RecordingRanker(private val answer: (RankRequest) -> Ranking) : SubstituteRanker {
    val requests = mutableListOf<RankRequest>()

    override fun rank(request: RankRequest): Ranking {
        requests += request
        return answer(request)
    }
}
