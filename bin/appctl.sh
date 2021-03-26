#!/usr/bin/env bash

[ $# -eq 0 ] && { echo "Usage: $0 [app_name] [build|deploy|run|delete|logs]"; exit 1; }
# check required env variables before proceeding
PARAM_1=$1
PARAM_2=$2
: "${PARAM_1:?ERROR: APP_NAME not provided !!!}"
: "${PARAM_2:?ERROR: MODE not provided !!!}"

export APP_NAME=$1
export MODE=$2

if [ $MODE == "logs" ]; then
  sparkctl log -f $APP_NAME

elif [ $MODE == "delete" ]; then
  sparkctl delete $APP_NAME

elif [ $MODE == "build" ]; then

  echo "Packaging assembly..."
  sbt assembly

elif [ $MODE == "deploy" ]; then
  echo "Copying assembly jar to GCS..."
  export JAR_FILE=$(find target/ -name "*assembly*.jar")
  #cleanup
  gsutil rm gs://${TF_VAR_project_name}/jars/baseline/$JAR_FILE
  rm -rf  ~/.gsutil/tracker-files/upload*
  gsutil cp $JAR_FILE gs://${TF_VAR_project_name}/jars/baseline/

elif [ $MODE == "run" ]; then

  echo "Deploying Spark app to Kubernetes..."
  kubectl label servicemonitor/prometheus-pushgateway release=prometheus-community --overwrite
  kubectl apply -f manifests/servicemonitor-spark.yaml
  sparkctl delete $APP_NAME
  #cleanup old Spark driver service if exists
  kubectl get svc --no-headers -o custom-columns=":metadata.name" | grep $APP_NAME-[[:alnum:]]*-driver-svc | xargs -I {} kubectl delete svc {}
  kubectl apply -f manifests/$APP_NAME.yaml
  sparkctl list

  while [ $(kubectl get svc --no-headers -o custom-columns=":metadata.name" | grep $APP_NAME-[[:alnum:]]*-driver-svc | wc -l) -lt 1 ];
  do
    echo "Service does not exists yet, waiting 1s"
    sleep 1
  done
  kubectl get svc --no-headers -o custom-columns=":metadata.name" | grep $APP_NAME-[[:alnum:]]*-driver-svc | xargs -I {} kubectl label svc {} sparkSvc=driver

fi