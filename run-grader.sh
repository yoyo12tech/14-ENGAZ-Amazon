#!/bin/bash
# run-grader.sh for Ahmed (Team Engaz)

# This prevents Git Bash from messing up the Windows paths sent to Docker
export MSYS_NO_PATHCONV=1

docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --tmpfs /grader-ram:exec,size=1024m \
  -v "D:/14-ENGAZ-Amazontest:/repo" \
  -v "$HOME/.m2:/root/.m2" \
  -e REPO_PATH=/repo \
  -e PROJECT=M2 \
  -e THEME=Amazon \
  -e DEBUG=1 \
  -e M2_NO_CASSANDRA_OVERRIDE=1 \
  -e M2_KEEP_DATA=1 \
  abuelmagd/scalable-grading-system:spring2026-latest