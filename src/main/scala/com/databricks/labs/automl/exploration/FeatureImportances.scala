package com.databricks.labs.automl.exploration

import com.databricks.labs.automl.exploration.structures.{
  FeatureImportanceConfig,
  FeatureImportanceOutput,
  FeatureImportanceReturn,
  FeatureImportanceTools
}
import com.databricks.labs.automl.model.{RandomForestTuner, XGBoostTuner}
import com.databricks.labs.automl.pipeline.FeaturePipeline
import com.databricks.labs.automl.sanitize.DataSanitizer
import com.databricks.labs.automl.utils.SparkSessionWrapper
import ml.dmlc.xgboost4j.scala.spark.{
  XGBoostClassificationModel,
  XGBoostRegressionModel
}
import org.apache.spark.ml.classification.RandomForestClassificationModel
import org.apache.spark.ml.regression.RandomForestRegressionModel
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

/**
  *
  * @param data
  * @param config
  * @param cutoffType
  * @param cutoffValue
  */
class FeatureImportances(data: DataFrame,
                         config: FeatureImportanceConfig,
                         cutoffType: String,
                         cutoffValue: Double)
    extends FeatureImportanceTools
    with SparkSessionWrapper {

  import com.databricks.labs.automl.exploration.structures.FeatureImportanceModelFamily._
  import com.databricks.labs.automl.exploration.structures.ModelType._
  import com.databricks.labs.automl.exploration.structures.CutoffTypes._

  private val cutOff = cutoffTypeEvaluator(cutoffType)
  private val modelFamily = featureImportanceFamilyEvaluator(
    config.featureImportanceModelFamily
  )
  private val modelType = modelTypeEvaluator(config.modelType)

  private def fillNaValues(): DataFrame = {

    val (cleanedData, fillMap, modelDetectedType) = new DataSanitizer(data)
      .setLabelCol(config.labelCol)
      .setFeatureCol(config.featuresCol)
      .setNumericFillStat(config.numericFillStat)
      .setCharacterFillStat(config.characterFillStat)
      .setModelSelectionDistinctThreshold(
        config.modelSelectionDistinctThreshold
      )
      .setFieldsToIgnoreInVector(config.fieldsToIgnore)
      .setParallelism(config.parallelism)
      .setFilterPrecision(0.01)
      .generateCleanData()

    cleanedData
  }

  private def createFeatureVector(df: DataFrame): FeatureImportanceOutput = {

    val (pipelinedData, fieldsInVector, allFields) = new FeaturePipeline(df)
      .setLabelCol(config.labelCol)
      .setFeatureCol(config.featuresCol)
      .setDateTimeConversionType(config.dateTimeConversionType)
      .makeFeaturePipeline(config.fieldsToIgnore)

    new FeatureImportanceOutput {
      override def data: DataFrame = pipelinedData

      override def fieldsInVector: Array[String] = fieldsInVector

      override def allFields: Array[String] = allFields
    }

  }

  private def getImportances(
    df: DataFrame,
    vectorFields: Array[String]
  ): Map[String, Double] = {

    val result = modelFamily match {
      case RandomForest =>
        val rfModel = new RandomForestTuner(df, config.modelType)
          .setLabelCol(config.labelCol)
          .setFeaturesCol(config.featuresCol)
          .setRandomForestNumericBoundaries(config.numericBoundaries)
          .setRandomForestStringBoundaries(config.stringBoundaries)
          .setScoringMetric(config.scoringMetric)
          .setTrainPortion(config.trainPortion)
          .setTrainSplitMethod(config.trainSplitMethod)
          .setTrainSplitChronologicalColumn(
            config.trainSplitChronologicalColumn
          )
          .setTrainSplitChronologicalRandomPercentage(
            config.trainSplitChronlogicalRandomPercentage
          )
          .setParallelism(config.parallelism)
          .setKFold(config.kFold)
          .setSeed(config.seed)
          .setOptimizationStrategy(config.scoringOptimizationStrategy)
          .setFirstGenerationGenePool(config.firstGenerationGenePool)
          .setNumberOfMutationGenerations(config.numberOfGenerations)
          .setNumberOfMutationsPerGeneration(
            config.numberOfMutationsPerGeneration
          )
          .setNumberOfParentsToRetain(config.numberOfParentsToRetain)
          .setGeneticMixing(config.geneticMixing)
          .setGenerationalMutationStrategy(config.generationalMutationStrategy)
          .setMutationMagnitudeMode(config.mutationMagnitudeMode)
          .setFixedMutationValue(config.fixedMutationValue)
          .setEarlyStoppingScore(config.autoStoppingScore)
          .setEarlyStoppingFlag(config.autoStoppingFlag)
          .setEvolutionStrategy(config.evolutionStrategy)
          .setContinuousEvolutionMaxIterations(
            config.continuousEvolutionMaxIterations
          )
          .setContinuousEvolutionStoppingScore(
            config.continuousEvolutionStoppingScore
          )
          .setContinuousEvolutionParallelism(
            config.continuousEvolutionParallelism
          )
          .setContinuousEvolutionMutationAggressiveness(
            config.continuousEvolutionMutationAggressiveness
          )
          .setContinuousEvolutionGeneticMixing(
            config.continuousEvolutionGeneticMixing
          )
          .setContinuousEvolutionRollingImporvementCount(
            config.continuousEvolutionRollingImprovementCount
          )
          .evolveBest()
          .model
        modelType match {
          case Regressor =>
            val importances = rfModel
              .asInstanceOf[RandomForestRegressionModel]
              .featureImportances
              .toArray
            vectorFields.zip(importances).toMap[String, Double]

          case Classifier =>
            val importances = rfModel
              .asInstanceOf[RandomForestClassificationModel]
              .featureImportances
              .toArray
            vectorFields.zip(importances).toMap[String, Double]
        }
      case XGBoost =>
        val xgModel = new XGBoostTuner(df, config.modelType)
          .setLabelCol(config.labelCol)
          .setFeaturesCol(config.featuresCol)
          .setXGBoostNumericBoundaries(config.numericBoundaries)
          .setScoringMetric(config.scoringMetric)
          .setTrainPortion(config.trainPortion)
          .setTrainSplitMethod(config.trainSplitMethod)
          .setTrainSplitChronologicalColumn(
            config.trainSplitChronologicalColumn
          )
          .setTrainSplitChronologicalRandomPercentage(
            config.trainSplitChronlogicalRandomPercentage
          )
          .setParallelism(config.parallelism)
          .setKFold(config.kFold)
          .setSeed(config.seed)
          .setOptimizationStrategy(config.scoringOptimizationStrategy)
          .setFirstGenerationGenePool(config.firstGenerationGenePool)
          .setNumberOfMutationGenerations(config.numberOfGenerations)
          .setNumberOfMutationsPerGeneration(
            config.numberOfMutationsPerGeneration
          )
          .setNumberOfParentsToRetain(config.numberOfParentsToRetain)
          .setGeneticMixing(config.geneticMixing)
          .setGenerationalMutationStrategy(config.generationalMutationStrategy)
          .setMutationMagnitudeMode(config.mutationMagnitudeMode)
          .setFixedMutationValue(config.fixedMutationValue)
          .setEarlyStoppingScore(config.autoStoppingScore)
          .setEarlyStoppingFlag(config.autoStoppingFlag)
          .setEvolutionStrategy(config.evolutionStrategy)
          .setContinuousEvolutionMaxIterations(
            config.continuousEvolutionMaxIterations
          )
          .setContinuousEvolutionStoppingScore(
            config.continuousEvolutionStoppingScore
          )
          .setContinuousEvolutionParallelism(
            config.continuousEvolutionParallelism
          )
          .setContinuousEvolutionMutationAggressiveness(
            config.continuousEvolutionMutationAggressiveness
          )
          .setContinuousEvolutionGeneticMixing(
            config.continuousEvolutionGeneticMixing
          )
          .setContinuousEvolutionRollingImporvementCount(
            config.continuousEvolutionRollingImprovementCount
          )
          .evolveBest()
          .model
        modelType match {
          case Regressor =>
            xgModel
              .asInstanceOf[XGBoostRegressionModel]
              .nativeBooster
              .getScore(vectorFields, "gain")
          case Classifier =>
            xgModel
              .asInstanceOf[XGBoostClassificationModel]
              .nativeBooster
              .getScore(vectorFields, "gain")
        }
    }
    result
  }

  private[exploration] def getTopFeaturesCount(
    featureDataFrame: DataFrame,
    featureCount: Int
  ): Array[String] = {
    featureDataFrame
      .sort(col("Importances").desc)
      .limit(featureCount)
      .collect()
      .map(x => x(0).toString)
  }

  private[exploration] def getTopFeaturesValue(
    featureDataFrame: DataFrame,
    importanceValue: Double
  ): Array[String] = {
    featureDataFrame
      .filter(col("Importance") >= importanceValue)
      .sort(col("Importance").desc)
      .collect()
      .map(x => x(0).toString)
  }

  private[exploration] def getAllImportances(
    featureDataFrame: DataFrame
  ): Array[String] = {
    featureDataFrame
      .sort(col("Importances").desc)
      .collect()
      .map(x => x(0).toString)
  }

  /**
    *
    * @return
    */
  def generateFeatureImportances(): FeatureImportanceReturn = {

    import spark.implicits._

    val cleanedData = fillNaValues()
    val vectorOutput = createFeatureVector(cleanedData)
    val importances =
      getImportances(vectorOutput.data, vectorOutput.fieldsInVector)

    val importancesDF = importances.toSeq
      .toDF("Feature", "Importance")
      .orderBy(col("Importance").desc)
      .withColumn("Importance", col("Importance") * 100.0)
      .withColumn("Feature", split(col("Feature"), "_si$")(0))

    val topFieldArray = cutOff match {
      case Count     => getTopFeaturesCount(importancesDF, cutoffValue.toInt)
      case Threshold => getTopFeaturesValue(importancesDF, cutoffValue)
      case None      => getAllImportances(importancesDF)
    }

    new FeatureImportanceReturn(
      importances = importancesDF,
      topFields = topFieldArray
    ) {
      override def data: DataFrame = vectorOutput.data

      override def fieldsInVector: Array[String] = vectorOutput.fieldsInVector

      override def allFields: Array[String] = vectorOutput.allFields
    }
  }
}

object FeatureImportances {

  def apply(data: DataFrame,
            config: FeatureImportanceConfig,
            cutoffType: String,
            cutoffValue: Double): FeatureImportances =
    new FeatureImportances(data, config, cutoffType, cutoffValue)

}
