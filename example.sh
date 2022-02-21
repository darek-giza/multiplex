#!/bin/bash

echo "Starting example usage ... "

now=$(date +%s000)
startAt=$((now + 360000))
finishAt=$((now + 72000000))

json="Content-Type: application/json"
getMoviesUrl="http://localhost:8080/movies?start=$startAt&finish=$finishAt"

echo GET all availavle projection from "$getMoviesUrl"
sleep 1

curl -s  -X GET -H "$json" "$getMoviesUrl" | jq -r

read PROJECTION_ID < <(curl -s  -X GET -H "$json" "$getMoviesUrl" | jq -r '.[].id')

movieDetailsUrl="http://localhost:8080/movies/details?id=$PROJECTION_ID"

echo GET details for projection with ID: $PROJECTION_ID  "$movieDetailsUrl"
sleep 1
curl -s  -X GET -H "$json" "$movieDetailsUrl" | jq -r

echo Take first free place and POST reservation "$movieDetailsUrl"
sleep 1

for i in {1..10}
do
  read ROW < <(curl -s  -X GET -H "$json" "$movieDetailsUrl" | jq -r '.places[].row')
  read SEAT < <(curl -s  -X GET -H "$json" "$movieDetailsUrl" | jq -r '.places[].seat')
  echo "--------------------------------------------------------------------------------"
  echo Place to reservation: "(" $ROW, $SEAT ")"
  echo "--------------------------------------------------------------------------------"

  orderDto="{\"id\": \"$PROJECTION_ID\",\"places\": [{\"place\": {\"row\": \"$ROW\",\"seat\": \"$SEAT\"},\"ticket\": \"ADULT\"}],\"name\": \"Dariusz Giza\"}"

  echo ""
  curl -H "$json" -d "$orderDto" "http://localhost:8080/order" | jq
  sleep 1
done

curl -s  -X GET -H "$json" "$movieDetailsUrl" | jq -r
