#!/bin/bash
set -euo pipefail

IMAGE_NAME="${IMAGE_NAME:-logistics-export}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .
