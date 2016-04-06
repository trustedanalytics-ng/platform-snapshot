#!/bin/bash
docker pull postgres
EXISTING_POSTGRES_ID=$(docker ps -a | grep 'snapshot-db-postgres' | cut -d " " -f 1)
if ! [ -z "$EXISTING_POSTGRES_ID" ]; then
   docker stop $EXISTING_POSTGRES_ID
   docker rm $EXISTING_POSTGRES_ID 
fi
CONTAINER=$(docker run --name snapshot-db-postgres -e POSTGRES_PASSWORD=postgres -d postgres)
CONTAINER_IP=$(docker inspect $CONTAINER | grep IPAddress | awk '{ print $2 }' | tr -d ',"')
echo "Postgres is running on address $CONTAINER_IP:5432"
export POSTGRES_JDBC_STRING=jdbc:postgresql://$CONTAINER_IP:5432/postgres

CF_COPYENV=`cf plugins|grep copyenv|wc -l`
if [ $CF_COPYENV -eq 0 ]; then 
  wget https://github.com/jthomas/copyenv/raw/master/bin/linux/copyenv
  cf install-plugin ./copyenv
  rm -fr ./copyenv
fi

ENV_STRING=`cf copyenv artifact-discovery`
VCAP=`echo $ENV_STRING|sed 's/[;]//g'`
`$VCAP`

echo $VCAP_SERVICES |jq .

mvn spring-boot:run -Dspring.profiles.active=local -Dserver.port=8080
