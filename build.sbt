name := "AutomatedML"

organization := "com.databricks"

version := "0.3.3"

scalaVersion := "2.11.12"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.0"
libraryDependencies += "org.mlflow" % "mlflow-client" % "0.8.0"
libraryDependencies += "com.databricks" % "dbutils-api_2.11" % "0.0.3"