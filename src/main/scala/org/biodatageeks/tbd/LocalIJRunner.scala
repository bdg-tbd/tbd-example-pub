package org.biodatageeks.tbd

import com.typesafe.scalalogging.Logger
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.{SequilaSession, SparkSession}
import org.biodatageeks.formats.BrowserExtensibleData
import org.biodatageeks.sequila.rangejoins.IntervalTree.Interval
import org.biodatageeks.sequila.utils.SequilaRegister
import org.biodatageeks.sequila.rangejoins.methods.IntervalTree.IntervalHolderChromosome
import org.biodatageeks.sequila.rangejoins.methods.IntervalTree.IntervalTreeJoinOptimChromosomeImpl.calcOverlap

import scala.collection.convert.ImplicitConversions.{`collection AsScalaIterable`, `iterator asScala`}
object LocalIJRunner {

  val logger = Logger("org.biodatageeks.tbd.IntervalJoinRun")


  def main(args: Array[String]): Unit = {

    System.setSecurityManager(null)

    lazy val spark: SparkSession = {
      SparkSession
        .builder()
        .master("local[1]")
        .getOrCreate()
    }

    val ss = SequilaSession(spark)
    SequilaRegister.register(ss)
//    spark.sparkContext.setLogLevel("WARN")

    val path1= "/Users/mwiewior/research/data/AIListTestData/chainRn4.bed"
    val path2= "/Users/mwiewior/research/data/AIListTestData/fBrain-DS14718.bed"

    val tab1 = "tab1"
    val tab2 = "tab2"
    ss.sql(
      s"""
         |CREATE TABLE IF NOT EXISTS $tab1
         |USING org.biodatageeks.sequila.datasources.BED.BEDDataSource
         |OPTIONS(path "$path1")
         |
      """.stripMargin)

    ss.sql(
      s"""
         |CREATE TABLE IF NOT EXISTS $tab2
         |USING org.biodatageeks.sequila.datasources.BED.BEDDataSource
         |OPTIONS(path "$path2")
         |
      """.stripMargin)

    /**
     * Here put your Interval implementation (class name)
     */
    val clazz = "org.biodatageeks.tbd.DummyIntervalHolder"
    import spark.implicits._
    val ds1 = ss
      .sql(s"SELECT * FROM $tab1")
      .as[BrowserExtensibleData]

    val ds2 = ss
      .sql(s"SELECT * FROM $tab2")
      .as[BrowserExtensibleData]

    val localIntervals = ds1
      .rdd
      .map( r=> (r.contig, Interval[Int](r.pos_start, r.pos_end), InternalRow.empty ) )
      .collect()

    val intervalTree = ss
      .time{
        logger.info("Started creating interval structure...")
        val tree = new IntervalHolderChromosome[InternalRow](localIntervals, clazz)
        spark.sparkContext.broadcast(tree)
      }

    logger.info("Started intervals intersection...")
    ss.time {
     val cnt = ds2.map(r => intervalTree.value.getIntervalTreeByChromosome(r.contig) match {
        case Some(t) => {
          val record = t.overlappers(r.pos_start, r.pos_end).toList
          record
            .flatMap(k => (k.getValue.map(_ => 1)))
        }
        case _ => List.empty
      })
       .flatMap(r=>r)
       .count()
      logger.info(s"Intervals cnt ${cnt}")
    }

    val query = s"""
                    | SELECT count(*) as cnt
                    | FROM $tab1 AS t1
                    | JOIN $tab2 AS t2 ON
                    | t1.contig = t2.contig AND
                    | t2.pos_end >= t1.pos_start AND
                    | t2.pos_start <= t1.pos_end""".stripMargin
    ss.time{
      logger.info(s"Running SeQuiLa join")
      ss
        .sql(query)
        .show()
    }
    spark.experimental.extraStrategies = Nil

    ss.time{
      logger.info(s"Running Spark join")
      ss
        .sql(query)
        .show()
    }
    spark.stop()
  }



}
