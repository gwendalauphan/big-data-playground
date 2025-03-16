package demo

import fonctionsUtils.UtilsSpark.{creerEtSetBasePardefaut, creerSparkSession}
import fonctionsUtils.UtilsFile.{readFile, writeSingleFile, countLines}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.log4j.Logger

import org.apache.spark.sql.functions._

object demoMain {

  val logger = Logger.getLogger(this.getClass.getName)

  def main(args: Array[String]): Unit = {

    ////////////////////////////////////////////
    /// CONF : Gestion des variables chemins ///
    ////////////////////////////////////////////

    val rootPath = this.getClass.getName.stripSuffix("$")
    
    val repertoireBaseSpark = if (args.length > 0) args(0) else s"$rootPath/repertoireBaseSpark"
    val baseSparkName       = if (args.length > 1) args(1) else "baseSpark"
    val pathData            = if (args.length > 2) args(2) else s"$rootPath/pathData"
    val pathResults         = if (args.length > 3) args(3) else s"$rootPath/pathResults"

    //Creation de la Session Spark
    val spark = creerSparkSession(repertoireBaseSpark, baseSparkName)

    //Affichage des chemins
    logger.info("Affichage des chemins")
    logger.info(s"rootPath : $rootPath")
    logger.info(s"repertoireBaseSpark : $repertoireBaseSpark")
    logger.info(s"baseSparkName : $baseSparkName")
    logger.info(s"pathData : $pathData")
    logger.info(s"pathResults : $pathResults")

    logger.info("Creation de la session Spark de l'application")
    val fileConf = spark.sparkContext.hadoopConfiguration.get("fs.defaultFS")
    logger.info(s"fileConf : $fileConf")

    //Création de la base de données et sélection
    logger.info("Creation et utilisation de la base de données Spark")
    creerEtSetBasePardefaut(baseSparkName)

    ////////////////////////////////////////////
    /// 1. Ouvrir ou créer le fichier CSV    ///
    ////////////////////////////////////////////

    val fileName = "temperature.csv"
    val filePath = new Path(s"$pathData/$fileName")
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)


    logger.info(s"pathData : $pathData")
    logger.info(s"pathResults : $pathResults")
    logger.info(s"fileName : $fileName")
    logger.info(s"filePath : $filePath")

    // Vérifier si le dossier existe
    if (!fs.exists(new Path(pathData))) {
        // créer le dossier
        logger.info(s"Le dossier $pathData n'existe pas. Création du dossier.")
        fs.mkdirs(new Path(pathData))
    }

    if (!fs.exists(new Path(pathResults))) {
        // créer le dossier
        logger.info(s"Le dossier $pathResults n'existe pas. Création du dossier.")
        fs.mkdirs(new Path(pathResults))
    }

    // Si le fichier n'existe pas, on le crée et on écrit quelques données bidon
    if (!fs.exists(filePath)) {
      logger.info(s"Le fichier $fileName n'existe pas. Creation du fichier avec des données exemple.")

      // Données bidon sans l'entête
      val data = Seq(
        "2025-03-16,15",
        "2025-03-17,17",
        "2025-03-18,10",
        "2025-03-19,19",
        "2025-03-20,14"
      )

      // On transforme en DataFrame
      val dfData = spark
        .createDataFrame(
            data.map { x =>
            val splitted = x.split(",")
            (splitted(0), splitted(1).toInt)
            }
      )
      .toDF("date", "temperature")

      dfData.show()

      // Ecriture du fichier CSV sur HDFS
      // writeSingleFile est supposée être une fonction utilitaire existante (UtilsFile)
      writeSingleFile(dfData, filePath.toString, spark.sparkContext, saveMode = "overwrite", format = "csv", delimiter = ";")
    } else {
      logger.info(s"Le fichier $fileName existe déjà. Lecture du contenu.")
    }

    ////////////////////////////////////////////
    /// 2. Implémentation de la logique      ///
    ////////////////////////////////////////////
    // Lecture du CSV en DataFrame Spark

    val dfTemp = readFile(spark, header = true, filePath.toString, format = "csv", delimiter = ";")

    // Exemple de traitement : calculer la température moyenne
    val dfAvg = dfTemp.agg(avg("temperature").alias("avg_temperature"))

    ////////////////////////////////////////////
    /// 3. Afficher le résultat             ///
    ////////////////////////////////////////////
    logger.info(s"Affichage du résultat (moyenne des températures) : ${dfAvg.show(false)}")
    

    ////////////////////////////////////////////
    /// 4. Enregistrer la table en Parquet   ///
    ////////////////////////////////////////////
    val parquetPath = s"$pathResults/temperature_parquet"
    logger.info(s"Enregistrement des données au format Parquet dans $parquetPath")

    // Pour la démo, on peut soit enregistrer la table transformée (dfAvg),
    // soit la table initiale (dfTemp). On choisit ici la table complète dfTemp.
    writeSingleFile(dfTemp, parquetPath, spark.sparkContext, saveMode = "overwrite", format = "parquet")

    // Fermeture de la session
    logger.info("Fin de la session")
    spark.stop()
  }
}
