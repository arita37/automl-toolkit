package com.databricks.labs.automl.pipeline

import org.apache.spark.ml.param.{Param, Params}

/**
  * @author Jas Bali
  *
  */
trait HasAutoMlIdColumn extends Params {

  final val automlInternalId: Param[String] = new Param[String](this, "automlInternalId", "unique identifier column internally generated by AutoML")

  def setAutomlInternalId(value: String): this.type = set(automlInternalId, value)

  def getAutomlInternalId: String = $(automlInternalId)

}
