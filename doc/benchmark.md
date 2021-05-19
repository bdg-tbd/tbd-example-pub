# Run TBD container
```bash
export IMAGE_TAG=0.1.3
export SEMESTER=tbd
export GROUP_ID=${SEMESTER}-devel
export PROJECT_DIR=$HOME/tbd/project
export APP_DIR=/Users/mwiewior/research/git/tbd-example-pub ### change it so that it points to your cloned project repo\n
export TF_VAR_billing_account=0139AC-53EC0E-245F75   ### copied from billing tab\n
export TF_VAR_location=europe-west1 ### St. Ghislain, Belgium\n
export TF_VAR_zone=europe-west1-b
export TF_VAR_machine_type=e2-standard-2

docker run --rm -it \
    --name tbd \
    -p 9090:9090 \
    -p 9091:9091 \
    -p 3000:3000 \
    -p 4040:4040 \
    -v $PROJECT_DIR:/home/tbd \
    -e GROUP_ID=$GROUP_ID \
    -e TF_VAR_billing_account=$TF_VAR_billing_account \
    -e TF_VAR_location=$TF_VAR_location \
    -e TF_VAR_zone=$TF_VAR_zone \
    -e TF_VAR_machine_type=$TF_VAR_machine_type \
    biodatageeks/tbd-os:$IMAGE_TAG bash
```

# Setup cluster (optional)
```bash
cd /home/git/tbd
git checkout 0.1.3
terraform init 
terraform apply -var-file=env/dev.tfvars -var 'max_node_count=5'
```

# Download test data
```bash
cd /home/tbd
wget http://big.databio.org/example_data/AIList/AIListTestData.tgz
tar zxvf AIListTestData.tgz
```

# Create test bucket and upload data
```bash
gcp-login.sh
cat > $HOME/.boto << EOF
  [GSUtil]
  parallel_composite_upload_threshold = 64M
EOF
gcloud config set project ${GOOGLE_PROJECT}
gsutil mb -l ${TF_VAR_location} gs://${GOOGLE_PROJECT}-bench
gsutil -m cp -r AIListTestData gs://${GOOGLE_PROJECT}-bench

```

# Add storage admin role to tbd-lab service account
```
gcloud auth login
gcloud config set project ${GOOGLE_PROJECT}
gcloud projects add-iam-policy-binding ${GOOGLE_PROJECT} \
  --member serviceAccount:tbd-lab@${GOOGLE_PROJECT}.iam.gserviceaccount.com \
  --role roles/storage.admin

```

# Deploy and run
```bash
export APP_NAME=tbd-999
bin/appctl.sh $APP_NAME deploy
bin/appctl.sh $APP_NAME run
sparkctl log -f $APP_NAME
```
# Check algorithm parameters
```bash
## Interval structure
21/05/18 20:06:52 INFO IntervalTreeJoinStrategyOptim: Running SeQuiLa interval join with following parameters:
minOverlap = 1
maxGap = 0
useJoinOrder = true
intervalHolderClassName = org.biodatageeks.sequila.rangejoins.methods.IntervalTree.IntervalTreeRedBlack

## Broadcast size 
21/05/19 04:58:31 INFO IntervalTreeJoinOptimChromosomeImpl$: 
Estimated broadcast structure size is ~ 46551.8 kb
spark.biodatageeks.rangejoin.maxBroadcastSize is set to 524288 kb"
Using JoinWithRowBroadcast join method


21/05/19 04:58:47 INFO IntervalTreeJoinOptimChromosomeImpl$: Real broadcast size of the interval structure is 44494600 bytes

```

# Once you've done with testing 
```
terraform apply -var-file=env/dev.tfvars -var 'max_node_count=3'
```