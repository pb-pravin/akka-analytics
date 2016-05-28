package akka.analytics.cassandra

import akka.analytics.cassandra.JournalContext.IsJournalContext
import com.datastax.spark.connector.ColumnRef
import com.typesafe.config.{Config, ConfigFactory}

object JournalContext {
  trait IsJournalContext[SC, R[_]] {
    def events(
        keyspace: String = "akka",
        table: String = "messages",
        serializer: EventSerializer,
        fields: Array[ColumnRef]
      ): R[(JournalKey, Any)]
  }

  private[cassandra] case object Ignore
}

private[cassandra] class JournalContext[SC, R[_], CTX <: IsJournalContext[SC, R]] (
    val sc: SC,
    serializerConfig: Config = ConfigFactory.empty()
  )(implicit ctx: CTX) {

  private var serializerOption: Option[EventSerializer] = None

  def withSerializerConfig(config: Config): JournalContext[SC, R, CTX] = {
    val journalContext = new JournalContext[SC, R, CTX](sc, config)(ctx)
    journalContext.serializerOption = serializerOption
    journalContext
  }

  def eventTable(
      keyspace: String = "akka",
      table: String = "messages"
    ): R[(JournalKey, Any)] = {

    val serializer = serializerOption match {
      case Some(c) => c
      case None =>
        val newSerializer = new EventSerializer(serializerConfig)
        serializerOption = Some(newSerializer)
        newSerializer
    }

    ctx.events(
      keyspace,
      table,
      serializer,
      Array("persistence_id", "partition_nr", "sequence_nr", "event", "ser_id", "ser_manifest", "message", "event_manifest", "writer_uuid"))
  }
}
