#!/usr/bin/env bash

SRC_BUCKET=gs://tbd-project
READS_PATH=data/reads/
TARGETS_PATH=data/targets/

: "${TF_VAR_project_name:?ERROR: TF_VAR_project_name !!!}"

PATH_DEST=gs://${TF_VAR_project_name}/data/
gsutil cp -R ${SRC_BUCKET}/${READS_PATH} ${PATH_DEST} && \
gsutil cp -R ${SRC_BUCKET}/${TARGETS_PATH} ${PATH_DEST}
if [ $? -eq 0 ]; then
  echo "Reads data copied to gs://${TF_VAR_project_name}"
else
  echo "Failed to copy test data to gs://${TF_VAR_project_name}"
fi
gsutil ls gs://${TF_VAR_project_name}/data/