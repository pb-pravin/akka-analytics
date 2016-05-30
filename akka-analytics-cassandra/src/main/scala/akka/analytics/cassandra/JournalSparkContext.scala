package akka.analytics.cassandra

import akka.analytics.cassandra.JournalContext.{Ignore, IsJournalContext}
import com.datastax.spark.connector._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class JournalSparkContext(
    sc: SparkContext,
    serializerConfig: Config = ConfigFactory.empty())
  extends IsJournalContext[RDD] {

  override def events(
      keyspace: String = "akka",
      table: String = "messages",
      serializer: EventSerializer,
      fields: Array[ColumnRef]
    ): RDD[(JournalKey, Any)] = {

    sc
      .cassandraTable(keyspace, table)
      .select(fields:_*)
      .map(serializer.deserialize)
      .filter(_._2 != Ignore)
  }
}
