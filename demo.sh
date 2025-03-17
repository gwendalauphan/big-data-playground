#!/bin/bash

set -eo pipefail

help() {
    echo "Usage: $0 [--rm]"
    echo "  --rm: Remove the containers after execution."
    echo "  --force-recreate: Force recreate the containers."
    exit 1
}

remove_containers=false
option=""

while [[ $# -gt 0 ]]; do
    case $1 in
        help)
            help
            ;;
        --rm)
            remove_containers=true
            shift
            ;;
        --force-recreate)
            option="--force-recreate"
            shift
            ;;
        *)
            echo "Unknown option: $1"
            help
            ;;
    esac
done

JAVA_VERSION=11.0.14.1
SBT_VERSION=1.6.2
SCALA_VERSION=2.12.15

SCALA_SHOT_VERSION=$(echo $SCALA_VERSION | cut -d. -f1,2)

packageName="demo_recette"
classMainName="demo.demoMain"
jarNamePrefix="${packageName}_${SCALA_SHOT_VERSION}"

dir_path=$(dirname $(realpath $0))
localjarDirPath="$dir_path/toolkit/target/scala-$SCALA_SHOT_VERSION"

jarFileName=$(ls $localjarDirPath | grep $jarNamePrefix)
localjarFilepath="$localjarDirPath/$jarFileName"

hdfsJarDirPath="/user/spark/jars"
hdfsJarFilepath="$hdfsJarDirPath/$jarFileName"

rootPath=$classMainName

echo_log() {
    echo "$(date) - $1"
}
# Affichage de la configuration
echo_log "Configuration:"
echo_log "JAVA_VERSION=$JAVA_VERSION"
echo_log "SBT_VERSION=$SBT_VERSION"
echo_log "SCALA_VERSION=$SCALA_VERSION"
echo_log "SCALA_SHOT_VERSION=$SCALA_SHOT_VERSION"
echo_log "packageName=$packageName"
echo_log "classMainName=$classMainName"
echo_log "jarNamePrefix=$jarNamePrefix"
echo_log "jarFileName=$jarFileName"
echo_log "localjarDirPath=$localjarDirPath"
echo_log "locallocaljarFilepath=$localjarFilepath"
echo_log "hdfsJarDirPath=$hdfsJarDirPath"
echo_log "hdfsJarFilepath=$hdfsJarFilepath"
echo


# Construire le JAR
echo_log "Building the JAR file..."
./toolkit/sbt.sh package JAVA_VERSION=$JAVA_VERSION SBT_VERSION=$SBT_VERSION SCALA_VERSION=$SCALA_VERSION
echo_log "JAR file built successfully."
echo
sleep 1

# Démarrer les conteneurs
echo_log "Starting the containers..."
cd hadoop-spark
docker compose up -d $option
cd ..
echo
echo_log "Waiting 10 seconds for the containers to start..."
sleep 10
echo_log "Containers started."

# Copier le JAR dans le conteneur hadoop-namenode
echo_log "Copying the JAR file to the hadoop-namenode container..."
docker cp $localjarFilepath hadoop-namenode:/opt/hadoop/data/nameNode/$jarFileName
echo_log "JAR file copied to the hadoop-namenode container."
echo
sleep 1

# Créer le répertoire $hdfsJarDirPath et copier le JAR dans le conteneur spark-master
echo_log "Creating $hdfsJarDirPath directory in hadoop-namenode container..."
docker exec -it hadoop-namenode bash -c "hdfs dfs -mkdir -p $hdfsJarDirPath"
echo_log "$hdfsJarDirPath directory created."
echo
sleep 1

# Copier le JAR dans HDFS
echo_log "Copying the JAR file to HDFS..."
docker exec -it hadoop-namenode bash -c "hdfs dfs -put -f /opt/hadoop/data/nameNode/$jarFileName $hdfsJarFilepath"
echo_log "JAR file copied to HDFS."
echo
sleep 1

# Soumettre le job spark (exécuter le JAR)
echo_log "Submitting the JAR file to Spark..."
docker exec -it spark-master bash -c "spark-submit --master spark://spark-master:7077 --class ${classMainName}  hdfs://hadoop-namenode:9000${hdfsJarFilepath}"
echo_log "Execution completed."
echo
sleep 1

# Afficher les résultats en lisant le répertoire $rootPath
echo_log "Results:"
docker exec -it hadoop-namenode bash -c "hdfs dfs -ls -R /user/spark/${rootPath}"
echo
sleep 1


if [ "$remove_containers" = true ]; then
    # Arrêter les conteneurs
    echo_log "Stopping the containers..."
    cd hadoop-spark
    docker compose down -v
    cd ..
    echo_log "Containers stopped."
    echo
    sleep 1
fi

