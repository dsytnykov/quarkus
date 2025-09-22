# Kubernetes Dashboard Setup on Docker Desktop

This guide explains how to deploy and access the Kubernetes Dashboard when using Docker Desktop's built-in Kubernetes cluster.

---

## 1. Deploy the Dashboard
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml
```

---

## 2. Create an Admin User

Save the following into a file called `dashboard-adminuser.yaml`:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: admin-user
  namespace: kubernetes-dashboard
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: admin-user-binding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: admin-user
  namespace: kubernetes-dashboard
```

Apply it:

```bash
kubectl apply -f dashboard-adminuser.yaml
```

---

## 3. Get the Login Token
```bash
kubectl -n kubernetes-dashboard create token admin-user
```

Copy the token, you will use it to log in.

---

## 4. Access the Dashboard
Run:

```bash
kubectl proxy
```

Then open in your browser:

```
http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
```

Paste the token when prompted.

---

## Additional Notes

- The Dashboard shows nodes, pods, deployments, services, and more.
- For real-time CPU/Memory metrics, install the Metrics Server:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

- Docker Desktop usually runs a single-node Kubernetes cluster, so you'll see one node in the Dashboard.
