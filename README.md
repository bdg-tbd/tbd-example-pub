# tbd-example-project

## How to setup a new project

https://github.com/MrPowers/spark-sbt.g8
```
sbt new MrPowers/spark-sbt.g8
```
## Core technologies

- OpenJDK: 11.0.10.hs-adpt
- Scala: 2.12.10
- Spark: 3.0.1
- sbt: 1.3.13

# Using a docker helper image
You can use the same docker image to build and deploy your app to GKE.
The only difference is that you need to mount 2 volumes :
- the one pointed by $PROJECT_DIR where you store your IaC
- the other one pointed by $APP_DIR where you have your sbt-based Spark app

You can clone your git repo into $APP_DIR and develop in your favourite IDE and only using container for deployment.
```

export IMAGE_TAG=0.1.3
export SEMESTER=2021l
export GROUP_ID=${SEMESTER}-999 ##Watch out ! Please use the group id provided by lecturers!!!
export PROJECT_DIR=$HOME/tbd/project
export APP_DIR=/Users/mwiewior/research/git/tbd-example-project ### change it so that it points to your cloned project repo
export TF_VAR_billing_account=011D36-51D2BA-441848   ### copied from billing tab
export TF_VAR_location=europe-west1 ### St. Ghislain, Belgium
export TF_VAR_zone=europe-west1-b
export TF_VAR_machine_type=e2-standard-2

mkdir -p $PROJECT_DIR
cd $PROJECT_DIR
docker run --rm -it \
    --name tbd \
    -p 9090:9090 \
    -p 9091:9091 \
    -p 3000:3000 \
    -v $PROJECT_DIR:/home/tbd \
    -v $APP_DIR:/home/app \
    -e GROUP_ID=$GROUP_ID \
    -e TF_VAR_billing_account=$TF_VAR_billing_account \
    -e TF_VAR_location=$TF_VAR_location \
    -e TF_VAR_zone=$TF_VAR_zone \
    -e TF_VAR_machine_type=$TF_VAR_machine_type \
    biodatageeks/tbd-os:$IMAGE_TAG bash
```
## GCP deployment 
Login to GKE:
```
gcp-login.sh
```

### Copy data to GCS
```bash
bin/prepare-test-data.sh
```

### appctl tool
```
bash-3.2$ bin/appctl.sh 
Usage: bin/appctl.sh [app_name] [build|deploy|run|delete|logs]

```
## Performance testing
Valid apps are : intervaljoin-spark and intervaljoin-sequila
### Build/ship
```
bin/appctl.sh intervaljoin-sequila build
```

### Deploy
```
bin/appctl.sh intervaljoin-sequila deploy
```
### Run
```
bin/appctl.sh intervaljoin-sequila run
```


## Prepare data
```bash
sdk use spark 3.0.1
spark-shell \
--driver-memory 8g \
--num-executors 4 \
--packages org.biodatageeks:sequila_2.12:0.6.5 -v
```

```scala
import org.apache.spark.sql.{SequilaSession, SparkSession}
import org.biodatageeks.sequila.utils.SequilaRegister

val tableName = "reads"
val bamPath = "/Users/mwiewior/research/data/NA12878.proper.wes.md.bam"

sql(
      s"""
         |CREATE TABLE IF NOT EXISTS $tableName
         |USING org.biodatageeks.sequila.datasources.BAM.BAMDataSource
         |OPTIONS(path "$bamPath")
         |
      """.stripMargin)

val df = spark
  .sql(s"SELECT sample_id,contig,pos_start,pos_end,cigar,seq FROM $tableName")

df.coalesce(32).write
  .partitionBy("sample_id","contig")
  .saveAsTable("reads_part")
```


### Spark image
```bash
cd docker
docker build -t biodatageeks/spark:v3.0.1-gcs-prometheus .
```

### Troubleshooting
Problems with not resolving deps in Spark Shell
```
rm -rf  ~/.ivy2/cache
rm -rf  ~/.m2/repository
```

## Results
```bash
+---------+
| count(1)|
+---------+
|132403544|
+---------+

```

# Prepare dataset
```
gsutil mb -l europe-west1 gs://tbd-project
gsutil cp -R spark-warehouse/reads_part/* gs://tbd-project/data/reads/
gsutil cp -R /Users/mwiewior/research/data/targets/tgp_exome_hg18.bed gs://tbd-project/data/targets/
gsutil iam ch allAuthenticatedUsers:objectViewer  gs://tbd-project/

```
