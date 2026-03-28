# Makefile Reference

Run `make` or `make help` to print this summary at any time.

---

## Development

| Command | Description |
|---|---|
| `make build` | Compile the backend (`mvn clean compile`) |
| `make dev-setup` | Full install without running tests (`mvn clean install -DskipTests`) |
| `make dev-run` | Start the backend with the `dev` profile (H2 in-memory DB, port 8080) |
| `make dev-test-watch` | Run tests without forking a new JVM — faster feedback loop |

---

## Testing

| Command | Description |
|---|---|
| `make test` | Run all backend unit + integration tests |
| `make test-integration` | Run TestContainers tests only (requires Docker) |
| `make test-all` | Clean build then run every test |

For the API and E2E suites run the test tools directly:

```bash
# Newman API tests  (backend must be running on port 8080)
cd testing/api && npm test

# Playwright E2E tests  (backend on 8080 + frontend on 3000)
cd testing/e2e && npx playwright test --timeout=30000
```

---

## Packaging

| Command | Description |
|---|---|
| `make package` | Build a runnable JAR, skip tests |
| `make package-with-tests` | Build a runnable JAR, run tests first |

The JAR lands at `backend/target/iam-server-*.jar`.

---

## Docker

| Command | Description |
|---|---|
| `make docker-build` | Build both `iam-server` and `iam-frontend` images tagged `1.0.0` |
| `make docker-build-latest` | Same but tagged `latest` |
| `make docker-push` | Build and push both images to `$(REGISTRY)` |
| `make docker-run` | Run the backend container locally (`localhost:8080`) |
| `make docker-stop` | Stop the locally running container |

Override the registry or version:

```bash
make docker-push REGISTRY=ghcr.io/your-org VERSION=v2.1.0
```

---

## Docker Compose

Copy `ops/.env.example` to `ops/.env` and fill in secrets before first run.

| Command | Description |
|---|---|
| `make compose-up` | Start the core stack (Postgres + backend + frontend) |
| `make compose-down` | Stop and remove containers |
| `make compose-logs` | Tail logs from all services |
| `make compose-rebuild` | Tear down, rebuild images, restart |

Start additional profiles:

```bash
# Prometheus + Grafana on port 3001
docker compose -f ops/docker-compose.yml --profile observability up -d

# Nginx reverse proxy
docker compose -f ops/docker-compose.yml --profile proxy up -d

# Everything at once
docker compose -f ops/docker-compose.yml --profile full up -d
```

---

## Kubernetes

The deploy script supports `dev`, `staging`, and `prod` environments via Kustomize overlays.

| Command | Description |
|---|---|
| `make k8s-deploy` | Apply manifests to the default namespace and image tag |
| `make k8s-deploy-dry` | Dry-run — print what would be applied |
| `make k8s-delete` | Remove all IAM resources from the cluster |
| `make k8s-status` | Show pod, service, and HPA status |
| `make k8s-logs` | Stream logs from the `iam-server` deployment |
| `make k8s-restart` | Rolling restart of the deployment |
| `make k8s-port-forward` | Forward `localhost:8080` → in-cluster service |

Target a specific environment directly with the deploy script:

```bash
# Dev cluster
ops/scripts/k8s-deploy.sh -e dev apply

# Staging with pinned tag
ops/scripts/k8s-deploy.sh -e staging -t v1.2.3 apply

# Production (prompts for confirmation on delete)
ops/scripts/k8s-deploy.sh -e prod -t v1.2.3 apply

# Check status
ops/scripts/k8s-deploy.sh -e prod status

# Roll back to previous revision
ops/scripts/k8s-deploy.sh -e prod rollback
```

Override the make variables for one-off deploys:

```bash
make k8s-deploy KUBE_NAMESPACE=my-namespace VERSION=v2.0.0
```

---

## Quality & Security

| Command | Description |
|---|---|
| `make lint` | Checkstyle + SpotBugs static analysis |
| `make security-scan` | OWASP dependency vulnerability scan |

---

## Cleanup

| Command | Description |
|---|---|
| `make clean` | Remove Maven build artifacts, prune Docker volumes, delete local image |

---

## Variable Overrides

All Makefile variables can be overridden on the command line:

| Variable | Default | Example override |
|---|---|---|
| `VERSION` | `1.0.0` | `VERSION=v2.1.0` |
| `REGISTRY` | `docker.io/vibes` | `REGISTRY=ghcr.io/your-org` |
| `KUBE_NAMESPACE` | `iam-system` | `KUBE_NAMESPACE=staging` |
