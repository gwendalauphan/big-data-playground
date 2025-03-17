# Big Data Playground
Le projet est un envrionnement de développement pour les technologies big data. Il est basé sur du spark, scala et hadoop hdfs.

Il permet d'appréhender le développement scala ainsi que de déployer un cluster hadoop et spark.

Le but est de se familiariser avec ces concepts, de jouer un peu avec et de voir comment ça marche.

Rappelons rapidement les technos:
- Hadoop: Projet open source qui fournit un ensemble de frameworks big data
- HDFS: Hadoop Distributed File System, un système de fichiers distribué
- Spark: Framework de traitement de données distribuées
- Scala: Langage de programmation
- SBT: Simple Build Tool, un outil de build pour scala
- Maven: Outil de build pour java

## Table des matières
- [Big Data Playground](#big-data-playground)
  - [Table des matières](#table-des-matières)
  - [Code Scala](#code-scala)
    - [Utilisation du Toolkit Docker](#utilisation-du-toolkit-docker)
      - [Configuration](#configuration)
  - [Hadoop \& Spark](#hadoop--spark)
    - [Lancement manuel](#lancement-manuel)
- [Demo](#demo)
  - [Déroulement de la démo](#déroulement-de-la-démo)
  - [Résultat](#résultat)
  - [Commandes utiles](#commandes-utiles)
- [Refs](#refs)
- [Repos source](#repos-source)

## Code Scala

Dans le dossier `app`, vous trouverez le code source de l'application scala.

Afin de **tester le package**, vous pouvez modifier le fichier
`app/src/main/scala/demo/demoMain.scala`
et lancer la commande :
```bash
sbt run
```
dans le **dossier racine du projet**.

Si vous souhaitez **changer la classe d’entrée par défaut**, éditez le fichier `build.sbt` et modifiez la ligne :
```scala
mainClass in (Compile, run) := Some("demo.demoMain")
```

---

### Utilisation du Toolkit Docker

Le projet propose un **toolkit** permettant de lancer le code Scala/Spark sans installer localement Java, Scala, SBT ni Spark.

> **Note** : Vous aurez besoin d’un compte Docker pour accéder au Hub Docker et, si nécessaire, pousser vos images. Les identifiants sont configurés dans le fichier `.env` (non versionné par git), au sein du dossier `toolkit`.

#### Configuration

1. Dans le dossier `toolkit/`, créez un fichier `.env` similaire à :
   ```
    DOCKER_USER=xxxxxxxx
    DOCKER_PASS=xxxxxxxxxxxx
    JAVA_VERSION=11.0.14.1
    SBT_VERSION=1.6.2
    SCALA_VERSION=2.12.15
   ```
   - Ajustez les versions par défaut de Java, Scala et SBT si besoin.

2. Rendez le script `sbt.sh` exécutable :
   ```bash
   chmod +x sbt.sh
   ```

## Hadoop & Spark

L'architecture du projet est basée sur un cluster Hadoop et Spark, composé de 4 containers :
- `hadoop-namenode` : le nœud maître HDFS
- `hadoop-datanode` : le nœud esclave HDFS
- `spark-master` : le nœud maître Spark
- `spark-worker` : le nœud esclave Spark

Le lancement se fait en utilisant `docker-compose` et entulisant des images Docker officielles. Les liens vers les images sont disponibles dans la section [Refs](#Refs).

Une configuration de base est fournie dans le dossier `hadoop_conf/` qui est partagé par hadoop et spark.

Voici l'architecture des containers:
![big_data_playbround.drawio](docs/big_data_playbround.drawio.png)
### Lancement manuel
```bash
cd hadoop-spark
docker-compose up
```

Ici, vous pouvez accéder aux interfaces web des services:
- UI hadoop Namenode: http://localhost:9870
- UI hadoop Datanode: http://localhost:9864 (ne fonctionne que si vous avez ajouté hadoop-datanode à /etc/hosts)
- UI spark master: http://localhost:8080

Note: Ajouter hadoop-datanode    127.0.0.1 dans `/etc/hosts` pour avoir accès à l'interface web du datanode.
Elle n'est utile que pour télécharger les fichiers depuis le datanode.

# Demo

La démo est un exemple simple de traitement de données avec Spark. Elle consiste à lire un fichier CSV, à effectuer un traitement simple et à sauvegarder le résultat dans un fichier Parquet.

Afin de lancer la démo, exécutez le script `demo.sh` la racine du projet.

```bash
./demo.sh --rm # pour supprimer les conteneurs après
```


```bash
./demo.sh help # pour afficher l'aide
help() {
    echo "Usage: $0 [--rm]"
    echo "  --rm: Remove the containers after execution."
    echo "  --force-recreate: Force recreate the containers."
    exit 1
}
```

## Déroulement de la démo

Le schéma ci-dessous illustre les grandes étapes :

```mermaid

graph TD
    A[Développement Scala<br/><em>app/src</em>] -->|lance| B[demo_sh]

    subgraph B[./demo_sh]
        
        subgraph C[./sbt_sh]
            D[Copy] --> E[build]
            E --> F[Compile]
        end
        subgraph G[docker_commands]

            H[Package Jar]--> I[Build Docker Image]
            I --> J[Containers Hadoop & Spark]
            J --> K[Copie Jar NameNode]
            K --> L[Dépôt Jar DataNode]
        end

        subgraph M[job_spark]

            N[Soumission Job Spark] --> O[Exécution Jar depuis HDFS]
            O --> P[Récupération données]
            P --> Q[Calcul Spark]
            Q --> R[Sauvegarde Parquet]
        end

        S[Affichage Résultats]
    end
        C --> G
        G --> M
        M --> S
        
    %% Style des noeuds avec des couleurs évolutives
    style A fill:#6FA8DC,color:#fff,stroke:#333,stroke-width:1px
    style B fill:#F5F5DC,color:#000000
    style D fill:#8A7DC5,color:#fff
    style E fill:#9675C7,color:#fff
    style F fill:#A26DCA,color:#fff
    style H fill:#3D85C6,color:#fff
    style I fill:#357DC8,color:#fff
    style J fill:#2D75CA,color:#fff
    style K fill:#2570CC,color:#fff
    style L fill:#1D6ACC,color:#fff
    style N fill:#990000,color:#fff
    style O fill:#B00000,color:#fff
    style P fill:#CC0000,color:#fff
    style Q fill:#E69138,color:#fff
    style R fill:#F6A24B,color:#fff
    style S fill:#6AA84F,color:#fff,stroke:#333,stroke-width:1px

```
## Résultat
[demo video](docs/demo_video_big_data_playground.mp4)

## Commandes utiles
```bash
# Lancer le cluster hadoop & spark
cd hadoop-spark
docker-compose up

# Construire le JAR
cd..
source toolkit/.env
./toolkit/sbt.sh package JAVA_VERSION=$JAVA_VERSION SBT_VERSION=$SBT_VERSION SCALA_VERSION=$SCALA_VERSION

# Copier le JAR dans le conteneur hadoop-namenode
docker cp ./toolkit/target/scala-2.12/demo_recette_2.12-0.1.jar hadoop-namenode:/opt/hadoop/data/nameNode/demo_recette_2.12-0.1.jar

# Créer le répertoire /user/spark/jars et copier le JAR dans le conteneur spark-master
docker exec -it hadoop-namenode bash -c "hdfs dfs -mkdir -p /user/spark/jars"


# Copier le JAR dans HDFS
docker exec -it hadoop-namenode bash -c "hdfs dfs -put /opt/hadoop/data/nameNode/demo_recette_2.12-0.1.jar /user/spark/jars"

# Soumettre le job spark (exécuter le JAR)
docker exec -it spark-master bash -c "spark-submit --class demo.demoMain --master spark://spark-master:7077 hdfs://hadoop-namenode:9000/user/spark/jars/demo_recette_2.12-0.1.jar"

# Afficher les résultats
docker exec -it hadoop-namenode bash -c "hdfs dfs -ls -R /user/spark/demo.demoMain"

# Arrêter les conteneurs
cd hadoop-spark
docker-compose down -v
```



# Refs

- **Hadoop**
  - [bigdatafoundation/docker-hadoop](https://github.com/bigdatafoundation/docker-hadoop)
  - [Article Medium sur la configuration d’un cluster HDFS Docker](https://bytemedirk.medium.com/setting-up-an-hdfs-cluster-with-docker-compose-a-step-by-step-guide-4541cd15b168)

- **Spark**
  - [Image Docker Bitnami Spark](https://hub.docker.com/r/bitnami/spark/tags)
  - [Github Bitnami Spark](https://github.com/bitnami/containers/tree/main/bitnami/spark)
  - https://github.com/tshine73/docker-spark-cluster/tree/master/
  - https://github.com/mvillarrealb/docker-spark-cluster/tree/master

- **Scala et SBT**
  - [Docker SBT](https://hub.docker.com/r/sbtscala/scala-sbt)
  - [GitHub Docker SBT](https://github.com/sbt/docker-sbt)

- **Maven Repository**
  - [mvnrepository.com](https://mvnrepository.com/)

# Repos source

Base: https://github.com/gwendalauphan/big-data-playground.git

