# Quarkus Microservices Demo

This project demonstrates **three Quarkus microservices** communicating via REST APIs, persisting to PostgreSQL, securing endpoints with **Keycloak (OIDC)**, and exchanging asynchronous events through **Kafka**. The whole system runs in **Kubernetes**.

## Architecture

- **user-service**

  - Exposes `/users` REST endpoints (using `quarkus-rest`).
  - Persists users in **PostgreSQL** (via Hibernate ORM Panache).
  - Emits Kafka messages (`user.events`) using **quarkus-messaging-kafka**.

- **order-service**

  - Exposes `/orders` REST endpoints.
  - Calls `user-service` via MicroProfile Rest Client to validate users.
  - Emits Kafka messages (`order.events`) using **quarkus-messaging-kafka**.

- **notification-service**

  - Consumes both `user.events` and `order.events` from Kafka.
  - Logs or simulates sending notifications.

- **Infrastructure**
  - PostgreSQL (Bitnami Helm chart).
  - Kafka (Strimzi operator).
  - Keycloak (OIDC for authentication and authorization).

---

## Prerequisites

- JDK 17+
- Maven
- Docker / Podman
- Kubernetes cluster (minikube/kind/k3s/real cluster)
- `kubectl`, `helm`
- Quarkus CLI (optional)

---

## 1. Create Quarkus projects

```bash
# user-service
mvn io.quarkus.platform:quarkus-maven-plugin:create \
  -DprojectGroupId=com.example -DprojectArtifactId=user-service \
  -Dextensions="quarkus-rest,hibernate-orm-panache,jdbc-postgresql,rest-jackson,quarkus-messaging-kafka,quarkus-oidc,container-image-docker,kubernetes"

# order-service
mvn io.quarkus.platform:quarkus-maven-plugin:create \
  -DprojectGroupId=com.example -DprojectArtifactId=order-service \
  -Dextensions="quarkus-rest,rest-client-jackson,rest-jackson,quarkus-messaging-kafka,quarkus-oidc,container-image-docker,kubernetes"

# notification-service
mvn io.quarkus.platform:quarkus-maven-plugin:create \
  -DprojectGroupId=com.example -DprojectArtifactId=notification-service \
  -Dextensions="quarkus-rest,quarkus-messaging-kafka,container-image-docker,kubernetes"
```

## 2. Create Database

Start DB:

```bash
helm install users-db bitnami/postgresql \
 --set global.postgresql.auth.username=demo_user \
 --set global.postgresql.auth.password=demo_pass \
 --set global.postgresql.auth.database=usersdb \
 --set global.postgresql.auth.postgresPassword=admin_pass
```

## 3. Create Kafka

Use this instruction to start kafka - https://strimzi.io/quickstarts/

After successful installation create topics:

```bash
kubectl apply -f kafka-topics.yaml
```

## 4. Create Keycloak

## 5. Build and push

## 6. Command for quick testing

## 7. Troubleshooting

You can start keycloak like localhost:8080 and login admin/admin using those commands:

1. Get the info for configuring (like port, etc.)

```bash
kubectl get svc -n keycloak
```

2. Forward to localhost

```bash
kubectl port-forward svc/keycloak 8080:80 -n keycloak - and
```

To check it from user-service use: kubectl exec -it deploy/user-service -- curl -v http://keycloak.keycloak.svc.cluster.local/realms/quarkus-realm/.well-known/openid-configuration

port 80

TOKEN=$(curl -s \ -d "client_id=user-service" \
 -d "username=alice" \
 -d "password=alicepass" \
 -d "grant_type=password" \
 http://keycloak:8080/realms/quarkus-realm/protocol/openid-connect/token \
 | grep -o '"access_token":"[^"]\*' | cut -d'"' -f4)

kubectl exec -it <user-service-pod> -- curl -X POST http://user-service:8080/users -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"username":"bob","email":"bob@example.com"}'

for quick test to receive a token:
kubectl port-forward svc/keycloak 8080:80 -n keycloak
and
curl -X POST \
 http://localhost:8080/realms/quarkus-realm/protocol/openid-connect/token \
 -H "Content-Type: application/x-www-form-urlencoded" \
 -d "grant_type=password" \
 -d "client_id=user-service" \
 -d "username=alice" \
 -d "password=alicepass"

kubectl scale deployment user-service --replicas=0

TOKEN=$(kubectl exec -it <any-pod> -- curl -s \
 -d "client_id=user-service" \
 -d "username=alice" \
 -d "password=alicepass" \
 -d "grant_type=password" \
 http://keycloak.keycloak.svc.cluster.local:8080/realms/quarkus-realm/protocol/openid-connect/token \
 | grep -o '"access_token":"[^"]\*' | cut -d'"' -f4)

curl -X POST http://localhost:30080/users \
 -H "Authorization: Bearer $TOKEN" \
 -H "Content-Type: application/json" \
 -d '{"username":"alice","email":"alice@example.com"}'

kubectl exec -it user-service-6d8bd8c647-l22ff -- curl -s -d "client_id=user-service" -d "username=alice" -d "password=alicepass" -d "grant_type=password" http://keycloak.keycloak.svc.cluster.local/realms/quarkus-realm/protocol/openid-connect/token | grep -o '"access_token":"[^"]\*' | cut -d'"' -f4

```

```
