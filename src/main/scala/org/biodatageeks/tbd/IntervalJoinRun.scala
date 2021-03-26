package org.biodatageeks.tbd

import com.typesafe.scalalogging.Logger
import htsjdk.samtools.ValidationStringency
import org.apache.spark.sql.{SequilaSession, SparkSession}
import org.biodatageeks.sequila.utils.{Columns, SequilaRegister}
import org.disq_bio.disq.HtsjdkReadsRddStorage
import org.rogach.scallop.ScallopConf

object IntervalJoinRun {

  val logger = Logger("org.biodatageeks.tbd.IntervalJoinRun")

  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val strategy = opt[String](required = false, default = Some("spark") )
    val prometheusUrl = opt[String](required = false, default = None)
    val readsPath = opt[String](required = true)
    val readsTableName = opt[String](required = true)
    val targetsPath = opt[String](required = true)
    val targetsTableName= opt[String](required = true)
    verify()
  }


  def main(args: Array[String]): Unit = {

    val conf = new Conf(args)
    System.setSecurityManager(null)

    lazy val spark: SparkSession = {
      SparkSession
        .builder()
        .enableHiveSupport()
        .getOrCreate()
    }

    val ss = SequilaSession(spark)
    SequilaRegister.register(ss)

    /***
     * Create an external table with 2-lvl partitioning (sample, contig name) - use reads* parameters as input
     */
    spark.sql(
      s"""""".stripMargin)

    /**
     * Refresh table partitions information in the metastore (i.e. register existing paths as partitions)
     */
    spark.sql(s"")

    /***
     * Create managed table using org.biodatageeks.sequila.datasources.BED.BEDDataSource for
     * a targets file in BED format (https://genome.ucsc.edu/FAQ/FAQformat.html#format1)
     * user targets* input parameters
     */
    spark.sql(s"""""".stripMargin)

    /**
     * Query1 - count the number of reads with a given cigar string and save the results in CSV format on GCS bucket
     */
    val query1 =
      s"""""".stripMargin

    /**
     * Query2 - count the number of intervals between reads and targets table and save results in Parquet format
     * Run the query using the Spark's builtin join algorithms and Sequila one
     */
    val query2 =
      s"""""".stripMargin

    logger.info(conf.summary)
    val stageMetrics = ch.cern.sparkmeasure.StageMetrics(spark)
    stageMetrics.begin()
    if(conf.strategy() == "spark") {
      // disable sequila strategy
      spark.experimental.extraStrategies = Nil
      spark.sql(query1).show()
    }
    else if (conf.strategy() == "sequila") {
      // do not check counts just join tables in a provided order (http://biodatageeks.ii.pw.edu.pl/sequila/architecture/architecture.html#optimizations)
      ss.sqlContext.setConf("spark.biodatageeks.rangejoin.useJoinOrder", "true")
      ss.sql(query1).show()
    }
    stageMetrics.`end`()
    if(conf.prometheusUrl()  != None) {
      logger.info("Sending metrics to Prometheus")
      //push metrics to Promethues gateway using https://github.com/LucaCanali/sparkMeasure
      //adjust the metrics labels accordingly
      stageMetrics.sendReportPrometheus(conf.prometheusUrl(),
        s"IntervalJoinRun-${conf.strategy()}", labelName = "run_id", System.currentTimeMillis().toString)
    }
    spark.stop()
  }

}
