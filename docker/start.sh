#!/bin/sh
# 법령 조회 서버는 백그라운드로 띄우고(백엔드의 기본 AXEDU_LAW_SEARCH_URL = http://127.0.0.1:8090),
# Spring Boot는 Railway가 넣어 주는 $PORT로 실행한다.
python3 ai/law_search_server.py --host 127.0.0.1 --port 8090 --data AI_DATA/chunks.jsonl &
exec java -XX:MaxRAMPercentage=75 -jar app.jar --server.port="${PORT:-8080}"
