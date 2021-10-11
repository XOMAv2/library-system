#!/bin/bash

docker-compose up -d
#kompose convert # превращает докер-файл в список сервисов
#kubectl create deployment auth --image=docker.io/xomav2/auth
#kubectl expose deployment auth --type=LoadBalancer --port=8080
#kubectl apply -f auth.yaml
#kubectl get deployments hello-node
#kubectl describe deployments hello-node
#kubectl apply -f ingress.yaml
#minikube service hello-node