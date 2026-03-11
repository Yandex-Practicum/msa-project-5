#!/bin/bash
set -e

NAMESPACE="transport-analytics"

echo "Deploying to Kubernetes..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/serviceaccount.yaml
kubectl apply -f k8s/cronjob.yaml

echo "Deployment completed!"
echo "Check status: kubectl get cronjob -n ${NAMESPACE}"
