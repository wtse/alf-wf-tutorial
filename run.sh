#!/bin/sh
set -e
set -u

start() {
    docker-compose up -d
}

down() {
    docker-compose down
}

purge() {
    rm -rf ./alf_data
}

# $1 is the composer name
# $2 is the docker image name
# $3 is the docker tag
build_component() {
    local name=$1
    local image_name=$2
    local tag=$3
    docker-compose kill $name || true
    docker-compose rm -f $name || true
    docker rmi "$image_name:$tag" || true
    docker-compose build $name
}

build_all() {
    mvn clean install -Pdocker -DskipTests=true -Dmaven.test.skip=true
    build_component "repository" "inexus.ixxus.co.uk:28443/ixxus/com.ixxus.wtse-repo" "development"
    build_component "share" "inexus.ixxus.co.uk:28443/ixxus/com.ixxus.wtse-share" "development"
}

tail() {
    docker-compose  logs -f
}

echo
echo "******************************************"
echo "Remeber to do:"
echo "    docker login inexus.ixxus.co.uk:28443"
echo "******************************************"
echo

case "$1" in
  build)
    down
    build_all
    start
    tail
    ;;
  loader)
    echo "Disabled!"
    start
    tail
    ;;
  repo)
    mvn clean install -DskipTests=true
    build_component "repository" "inexus.ixxus.co.uk:28443/ixxus/com.ixxus.wtse-repo" "development"
    start
    tail
    ;;
  share)
    mvn clean install -DskipTests=true
    build_component "share" "inexus.ixxus.co.uk:28443/ixxus/com.ixxus.wtse-share" "development"
    start
    tail
    ;;
  start)
    start
    tail
    ;;
  stop)
    down
    ;;
  purge)
    down
    purge
    ;;
  tail)
    tail
    ;;
  *)
    echo "Usage: $0 {build_start|start|stop|purge|tail|loader|configurator|controller|processor}"
esac