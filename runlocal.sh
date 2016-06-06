#!/bin/bash
#
# Copyright (c) 2015 Intel Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


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

#ENV_STRING=`cf copyenv platform-snapshot`
#VCAP=`echo $ENV_STRING|sed 's/[;]//g'`
#`$VCAP`

echo $VCAP_SERVICES

mvn spring-boot:run -Dspring.profiles.active=local -Dserver.port=8080
