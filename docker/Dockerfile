FROM gcr.io/spark-operator/spark:v3.0.0-gcs-prometheus

ARG SPARK_VERSION=3.0.1
ARG HADOOP_VERSION=2.7
ARG GCS_CONNECTOR_VERSION=hadoop2-1.9.17
ARG GCS_NIO_VERSION=0.120.0-alpha


RUN rm -rf /opt/spark
RUN apt update && apt install wget -y

# Spark installation
WORKDIR /tmp
# Using the preferred mirror to download Spark
# hadolint ignore=SC2046
RUN wget -q https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz
RUN tar xzf "spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz" -C /opt --owner root --group root --no-same-owner && \
    rm "spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz"

WORKDIR /opt
RUN  mv spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}/* spark/ && rm -rf spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}/

# Configure Spark
ENV SPARK_HOME=/opt/spark

RUN cd $SPARK_HOME/jars && wget https://repo1.maven.org/maven2/com/google/cloud/bigdataoss/gcs-connector/${GCS_CONNECTOR_VERSION}/gcs-connector-${GCS_CONNECTOR_VERSION}-shaded.jar && \
    wget https://repo1.maven.org/maven2/com/google/cloud/google-cloud-nio/${GCS_NIO_VERSION}/google-cloud-nio-${GCS_NIO_VERSION}-shaded.jar
