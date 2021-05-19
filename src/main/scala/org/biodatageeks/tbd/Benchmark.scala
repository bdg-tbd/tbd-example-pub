package org.biodatageeks.tbd

import com.typesafe.scalalogging.Logger
import org.apache.spark.sql.{SequilaSession, SparkSession}
import org.biodatageeks.sequila.utils.{InternalParams, SequilaRegister}
import org.rogach.scallop.ScallopConf

import java.util.concurrent.TimeUnit.NANOSECONDS

object Benchmark {

  val logger = Logger("org.biodatageeks.tbd.Benchmark")

  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val intervalHolderClass = opt[String](required = false,
      default = Some("org.biodatageeks.sequila.rangejoins.methods.IntervalTree.IntervalTreeRedBlack") )
    val prometheusUrl = opt[String](required = false, default = None)
    val leftTablePath = opt[String](required = true)
    val leftTableName = opt[String](required = true)
    val rightTablePath = opt[String](required = true)
    val rightTableName= opt[String](required = true)
    verify()
  }

  def main(args: Array[String]): Unit = {
    System.setSecurityManager(null)
    val conf = new Conf(args)


    lazy val spark: SparkSession = {
      SparkSession
        .builder()
        .getOrCreate()
    }
    val ss = SequilaSession(spark)
    SequilaRegister.register(ss)

    ss.sql(
      s"""
         |CREATE TABLE IF NOT EXISTS ${conf.leftTableName()}
         |USING org.biodatageeks.sequila.datasources.BED.BEDDataSource
         |OPTIONS(path "${conf.leftTablePath()}")
         |
      """.stripMargin)

    ss.sql(
      s"""
         |CREATE TABLE IF NOT EXISTS ${conf.rightTableName()}
         |USING org.biodatageeks.sequila.datasources.BED.BEDDataSource
         |OPTIONS(path "${conf.rightTablePath()}")
         |
      """.stripMargin)

    //use your group id
    val GROUP_ID = "TBD-999"
    //do not check tables' count broadcast right table
    ss.sqlContext.setConf(InternalParams.useJoinOrder, "true")

    //set max broadcast size to 512M
    ss.sqlContext.setConf(InternalParams.maxBroadCastSize, (512*1024*1024).toString)

    //set your class for holding intervals
    ss.sqlContext.setConf(InternalParams.intervalHolderClass, conf.intervalHolderClass() )
    val query = s"""
                   | SELECT count(*) as cnt
                   | FROM ${conf.leftTableName()} AS t1
                   | JOIN ${conf.rightTableName()} AS t2 ON
                   | t1.contig = t2.contig AND
                   | t2.pos_end >= t1.pos_start AND
                   | t2.pos_start <= t1.pos_end""".stripMargin

      logger.info(s"Running SeQuiLa join")
      val stageMetrics = ch.cern.sparkmeasure.StageMetrics(ss)
      stageMetrics.begin()
      ss
        .sql(query)
        .show()
      stageMetrics.end()
    val lId = conf.leftTablePath().split('/').last.split('.').head
    val rId = conf.rightTablePath().split('/').last.split('.').head
    val algoId = conf.intervalHolderClass().split('.').last
    val instanceNum = spark.conf.get("spark.executor.instances")
    stageMetrics
      .sendReportPrometheus(
        conf.prometheusUrl(),
        s"SeQuiLa-${GROUP_ID}-${algoId}-${lId}-${rId}-${instanceNum}",
        labelName = "run_id",
        System.currentTimeMillis().toString)
  }


}
