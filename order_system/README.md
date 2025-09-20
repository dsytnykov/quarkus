Start DB:
helm install users-db bitnami/postgresql \
 --set global.postgresql.auth.username=demo_user \
 --set global.postgresql.auth.password=demo_pass \
 --set global.postgresql.auth.database=usersdb \
 --set global.postgresql.auth.postgresPassword=admin_pass

kubectl port-forward svc/keycloak 8080:80 -n keycloak - and you can start keycloak like localhost:8080 and login admin/admin

kubectl get svc -n keycloak

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
