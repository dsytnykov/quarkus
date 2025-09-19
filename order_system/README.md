Start DB:
helm install users-db bitnami/postgresql \
 --set global.postgresql.auth.username=demo_user \
 --set global.postgresql.auth.password=demo_pass \
 --set global.postgresql.auth.database=usersdb \
 --set global.postgresql.auth.postgresPassword=admin_pass
