name := "demo_recette"

version := "0.1"

scalaVersion := "2.12.15"

val sparkVersion = "3.0.1"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-hive" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.scalatest" %% "scalatest" % "3.2.2" % Test
)


//lancement de la classe Main principale
mainClass in (Compile, run) := Some("demo.demoMain")
// Compatibilite sbt et Spark en local

fork := true // permet de separer le processus sbt et spark

connectInput in run := true // connecte la sortie standard a sbt pendant le run
outputStrategy := Some(StdoutOutput) // supprime les prefixes pour les logs non-sbt
