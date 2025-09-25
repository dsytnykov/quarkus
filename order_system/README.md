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
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

helm install users-db bitnami/postgresql \
 --set global.postgresql.auth.username=demo_user \
 --set global.postgresql.auth.password=demo_pass \
 --set global.postgresql.auth.database=usersdb \
 --set global.postgresql.auth.postgresPassword=admin_pass
```

This creates users-db-postgresql service. Quarkus quarkus.datasource.jdbc.url should point to users-db-postgresql.default.svc.cluster.local:5432 (or simply users-db-postgresql:5432 in the same namespace). Using the Bitnami chart is recommended for dev/test.
https://travis.media/blog/installing-postgres-bitnami-helm-chart/?utm_source=chatgpt.com

## 3. Create Kafka

Use this instruction to start kafka - https://strimzi.io/quickstarts/

After successful installation create topics:

```bash
kubectl apply -f kafka-topics.yaml
```

## 4. Create Keycloak

For dev/test use the Keycloak quickstart YAML:

```bash
kubectl create ns keycloak
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/main/kubernetes/keycloak.yaml -n keycloak
```

That quickstart creates a single Keycloak instance (admin/admin). In production use the Keycloak Operator / Helm. - https://www.keycloak.org/getting-started/getting-started-kube?utm_source=chatgpt.com

### Configure realm

1. Create realm `quarkus-realm`.

2. Create a client `user-service` (OpenID client): usually set Access Type to confidential or public depending on your flow (for resource servers you simply need tokens to be validated). For testing you can use a public client plus password grant for test users (Grant type), or use client credentials.

3. Create a test user alice and set a password. Mark the email as verified and password as not temporary. Give her role user.

4. Create clients for `order-service` if you will call it with tokens.

### Start keycloak like localhost:8080 and login admin/admin:

1. Get the info for configuring (like port, etc.)

```bash
kubectl get svc -n keycloak
```

2. Forward to localhost:8080

```bash
kubectl port-forward svc/keycloak 8080:80 -n keycloak
```

Open localhost:8080 and use admin/admin to come into a dashboard

To check that everything works from user-service use:

```bash
kubectl exec -it deploy/user-service -- curl -v http://keycloak.keycloak.svc.cluster.local/realms/quarkus-realm/.well-known/openid-configuration
```

## 5. Build and push

To build a project you can use

```bash
./mvnw package -DskipTests

docker push your-registry/your-user-service:0.1.0

```

I've used hub.docker.com and pushed through Docker Desktop interface

You can have Quarkus generate Dockerfiles and build images locally with the Docker extension:

```bash
# from each service folder, e.g. user-service
./mvnw package -Dquarkus.container-image.build=true \
  -Dquarkus.container-image.image=your-registry/your-user-service:0.1.0
```

## 6. Deploy sequence

Deploy sequence (recommended)

1. Install Strimzi operator (namespace kafka) and create Kafka cluster + topics.

2. Install Bitnami Postgres (helm install users-db bitnami/postgresql ...).

3. Deploy Keycloak (quickstart or operator).

4. Build images for each service and push to a registry accessible by the cluster (docker build / mvn -Dquarkus.container-image.build=true etc.)

5. Create Kubernetes.

6. Apply the service Deployments and Services.

7. Check logs of notification-service — it should connect to Kafka and wait for messages.

8. Use Keycloak to get a test token (password grant), call user-service or order-service with Authorization: Bearer <token> to exercise flow. You should see notification logs with created events.

## 7. Command for quick testing

### Testing in command line

```bash

kubectl apply -f user-service.yaml
kubectl apply -f order-service.yaml
kubectl apply -f notification-service.yaml

TOKEN=$(curl -s \ -d "client_id=user-service" \
 -d "username=alice" \
 -d "password=alicepass" \
 -d "grant_type=password" \
 http://keycloak.keycloak.svc.cluster.local:80/realms/quarkus-realm/protocol/openid-connect/token \
 | grep -o '"access_token":"[^"]\*' | cut -d'"' -f4)

# or from inside a pod
TOKEN=$(kubectl exec -it <any-pod> -- curl -s \
 -d "client_id=user-service" \
 -d "username=alice" \
 -d "password=alicepass" \
 -d "grant_type=password" \
 http://keycloak.keycloak.svc.cluster.local:80/realms/quarkus-realm/protocol/openid-connect/token \
 | grep -o '"access_token":"[^"]\*' | cut -d'"' -f4)


curl -X POST http://localhost:30080/users \
 -H "Authorization: Bearer $TOKEN" \
 -H "Content-Type: application/json" \
 -d '{"username":"alice","email":"alice@example.com"}'

# get pods
kubectl get pods

# check logs
kubectl logs deploy/notification-service
# or
kubectl logs pod <pod>

# scaling
kubectl scale deployment user-service --replicas=0

 # restart
kubectl rollout restart deployment user-service
```

### Testing from Postman

It is configured a nodeport for user-service - 30080 and order-service - 30081. You can use localhost:30080 in postman with authentication.

You can pick up an access token with using a command:

```bash
kubectl exec -it <service-pod> -- curl -s   -d "client_id=<clien-id>"  -d "username=alice"   -d "password=alicepass"   -d "grant_type=password"   http://keycloak.keycloak.svc.cluster.local/realms/quarkus-realm/protocol/openid-connect/token   | grep -o '"access_token":"[^"]*' | cut -d'"' -f4
```

And use this token in postman with selecting 'Bearer Token' for authentication and pasting access token.

For user service a body raw->json:

```json
{
  "username": "alice",
  "email": "alice@gmail.com"
}
```

For order-service a body raw->json:

```json
{
  "orderId": "1",
  "userId": "1"
}
```

## 8. Troubleshooting

## 9. Future changes / ideas

1. Configure authenticated service-to-service communication (for now it uses public /users/{id} endpoint)
