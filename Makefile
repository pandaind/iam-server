# Makefile for IAM Server
# Provides convenient commands for building, testing, and deploying

# Variables
APP_NAME := iam-server
VERSION := 1.0.0
REGISTRY := docker.io/vibes
IMAGE_NAME := $(REGISTRY)/$(APP_NAME)
KUBE_NAMESPACE := iam-system

# Directory shortcuts
BACKEND_DIR := backend
OPS_DIR := ops

# Colors for output
GREEN := \033[0;32m
YELLOW := \033[1;33m
RED := \033[0;31m
NC := \033[0m # No Color

.PHONY: help build test package docker-build docker-push k8s-deploy k8s-delete clean

# Default target
help: ## Show this help message
	@echo "IAM Server Build and Deployment Commands"
	@echo ""
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Development Commands
build: ## Build the application
	@echo "$(GREEN)Building application...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml clean compile

test: ## Run tests
	@echo "$(GREEN)Running tests...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml test

test-integration: ## Run integration tests with TestContainers
	@echo "$(GREEN)Running integration tests...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml test -Dtest="*TestContainer*"

test-all: ## Run all tests including integration
	@echo "$(GREEN)Running all tests...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml clean test

package: ## Package the application
	@echo "$(GREEN)Packaging application...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml clean package -DskipTests

package-with-tests: ## Package the application with tests
	@echo "$(GREEN)Packaging application with tests...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml clean package

# Docker Commands
docker-build: ## Build Docker image
	@echo "$(GREEN)Building Docker image...$(NC)"
	./$(OPS_DIR)/scripts/docker-build.sh -t $(VERSION)

docker-build-latest: ## Build Docker image with latest tag
	@echo "$(GREEN)Building Docker image with latest tag...$(NC)"
	./$(OPS_DIR)/scripts/docker-build.sh -t latest

docker-push: ## Build and push Docker image
	@echo "$(GREEN)Building and pushing Docker image...$(NC)"
	./$(OPS_DIR)/scripts/docker-build.sh -t $(VERSION) -r $(REGISTRY) -p

docker-run: ## Run Docker container locally
	@echo "$(GREEN)Running Docker container...$(NC)"
	docker run -p 8080:8080 --name $(APP_NAME) --rm $(APP_NAME):$(VERSION)

docker-stop: ## Stop running Docker container
	@echo "$(YELLOW)Stopping Docker container...$(NC)"
	docker stop $(APP_NAME) || true

# Docker Compose Commands
compose-up: ## Start services with Docker Compose
	@echo "$(GREEN)Starting services with Docker Compose...$(NC)"
	docker-compose -f $(OPS_DIR)/docker-compose.yml up -d

compose-down: ## Stop services with Docker Compose
	@echo "$(YELLOW)Stopping services with Docker Compose...$(NC)"
	docker-compose -f $(OPS_DIR)/docker-compose.yml down

compose-logs: ## Show Docker Compose logs
	@echo "$(GREEN)Showing Docker Compose logs...$(NC)"
	docker-compose -f $(OPS_DIR)/docker-compose.yml logs -f

compose-rebuild: ## Rebuild and restart services
	@echo "$(GREEN)Rebuilding and restarting services...$(NC)"
	docker-compose -f $(OPS_DIR)/docker-compose.yml down
	docker-compose -f $(OPS_DIR)/docker-compose.yml build --no-cache
	docker-compose -f $(OPS_DIR)/docker-compose.yml up -d

# Kubernetes Commands
k8s-deploy: ## Deploy to Kubernetes
	@echo "$(GREEN)Deploying to Kubernetes...$(NC)"
	./$(OPS_DIR)/scripts/k8s-deploy.sh apply -n $(KUBE_NAMESPACE) -t $(VERSION)

k8s-deploy-dry: ## Dry run Kubernetes deployment
	@echo "$(GREEN)Dry run Kubernetes deployment...$(NC)"
	./$(OPS_DIR)/scripts/k8s-deploy.sh apply -n $(KUBE_NAMESPACE) -t $(VERSION) --dry-run

k8s-delete: ## Delete from Kubernetes
	@echo "$(YELLOW)Deleting from Kubernetes...$(NC)"
	./$(OPS_DIR)/scripts/k8s-deploy.sh delete -n $(KUBE_NAMESPACE)

k8s-status: ## Show Kubernetes deployment status
	@echo "$(GREEN)Showing Kubernetes status...$(NC)"
	./$(OPS_DIR)/scripts/k8s-deploy.sh status -n $(KUBE_NAMESPACE)

k8s-logs: ## Show Kubernetes logs
	@echo "$(GREEN)Showing Kubernetes logs...$(NC)"
	kubectl logs -f deployment/iam-server -n $(KUBE_NAMESPACE)

k8s-restart: ## Restart Kubernetes deployment
	@echo "$(GREEN)Restarting Kubernetes deployment...$(NC)"
	kubectl rollout restart deployment/iam-server -n $(KUBE_NAMESPACE)

k8s-port-forward: ## Port forward to local machine
	@echo "$(GREEN)Port forwarding to localhost:8080...$(NC)"
	kubectl port-forward svc/iam-server-service 8080:8080 -n $(KUBE_NAMESPACE)

# Development Shortcuts
dev-setup: ## Set up development environment
	@echo "$(GREEN)Setting up development environment...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml clean install -DskipTests

dev-run: ## Run application in development mode
	@echo "$(GREEN)Running application in development mode...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev

dev-test-watch: ## Run tests in watch mode
	@echo "$(GREEN)Running tests in watch mode...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml test -Dspring.profiles.active=test -DforkMode=never

# Quality Checks
lint: ## Run code quality checks
	@echo "$(GREEN)Running code quality checks...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml checkstyle:check spotbugs:check

security-scan: ## Run security vulnerability scan
	@echo "$(GREEN)Running security scan...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml dependency-check:check

# Cleanup Commands
clean: ## Clean build artifacts
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml clean
	docker system prune -f --volumes
	docker image rm $(APP_NAME):$(VERSION) || true
	docker image rm $(APP_NAME):latest || true

clean-all: ## Clean everything including Docker
	@echo "$(RED)Cleaning everything...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml clean
	docker-compose -f $(OPS_DIR)/docker-compose.yml down -v --remove-orphans
	docker system prune -af --volumes
	kubectl delete namespace $(KUBE_NAMESPACE) --ignore-not-found=true

# Utility Commands
logs: ## Show application logs (local)
	@echo "$(GREEN)Showing application logs...$(NC)"
	tail -f logs/iam-server.log

health: ## Check application health
	@echo "$(GREEN)Checking application health...$(NC)"
	curl -f http://localhost:8080/actuator/health || echo "$(RED)Application not healthy$(NC)"

version: ## Show version information
	@echo "Application: $(APP_NAME)"
	@echo "Version: $(VERSION)"
	@echo "Registry: $(REGISTRY)"
	@echo "Image: $(IMAGE_NAME):$(VERSION)"
	@echo "Namespace: $(KUBE_NAMESPACE)"

# CI/CD Pipeline Commands
ci-test: ## Run CI test pipeline
	@echo "$(GREEN)Running CI test pipeline...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml clean test jacoco:report

ci-build: ## Run CI build pipeline
	@echo "$(GREEN)Running CI build pipeline...$(NC)"
	mvn -f $(BACKEND_DIR)/pom.xml clean package -DskipTests
	./$(OPS_DIR)/scripts/docker-build.sh -t $(VERSION)

ci-deploy: ## Run CI deploy pipeline
	@echo "$(GREEN)Running CI deploy pipeline...$(NC)"
	./$(OPS_DIR)/scripts/docker-build.sh -t $(VERSION) -r $(REGISTRY) -p
	./$(OPS_DIR)/scripts/k8s-deploy.sh apply -n $(KUBE_NAMESPACE) -t $(VERSION)

# Frontend Commands
FRONTEND_DIR := frontend

frontend-install: ## Install frontend dependencies
	@echo "$(GREEN)Installing frontend dependencies...$(NC)"
	npm --prefix $(FRONTEND_DIR) install

frontend-dev: ## Start frontend development server
	@echo "$(GREEN)Starting frontend development server on :3000...$(NC)"
	npm --prefix $(FRONTEND_DIR) run dev

frontend-build: ## Build frontend for production
	@echo "$(GREEN)Building frontend...$(NC)"
	npm --prefix $(FRONTEND_DIR) run build

frontend-lint: ## Lint frontend code
	@echo "$(GREEN)Linting frontend...$(NC)"
	npm --prefix $(FRONTEND_DIR) run lint

frontend-type-check: ## TypeScript type-check frontend
	@echo "$(GREEN)Type-checking frontend...$(NC)"
	npm --prefix $(FRONTEND_DIR) run type-check

frontend-docker-build: ## Build frontend Docker image
	@echo "$(GREEN)Building frontend Docker image...$(NC)"
	docker build -t iam-ui:$(VERSION) $(FRONTEND_DIR)

# API Testing (Newman)
API_TEST_DIR := testing/api

api-test-install: ## Install Newman API test dependencies
	@echo "$(GREEN)Installing Newman dependencies...$(NC)"
	npm --prefix $(API_TEST_DIR) install

api-test: ## Run API tests against local server
	@echo "$(GREEN)Running API tests (local)...$(NC)"
	npm --prefix $(API_TEST_DIR) run test:local

api-test-docker: ## Run API tests against Docker stack
	@echo "$(GREEN)Running API tests (Docker)...$(NC)"
	npm --prefix $(API_TEST_DIR) run test:docker

api-test-ci: ## Run API tests in CI mode (JUnit XML output)
	@echo "$(GREEN)Running API tests (CI)...$(NC)"
	npm --prefix $(API_TEST_DIR) run test:ci

# E2E Testing (Playwright)
E2E_DIR := testing/e2e

e2e-install: ## Install Playwright and browsers
	@echo "$(GREEN)Installing Playwright...$(NC)"
	npm --prefix $(E2E_DIR) install
	npm --prefix $(E2E_DIR) run install:browsers

e2e-test: ## Run all E2E tests (headless)
	@echo "$(GREEN)Running E2E tests...$(NC)"
	npm --prefix $(E2E_DIR) test

e2e-test-headed: ## Run E2E tests in headed mode
	@echo "$(GREEN)Running E2E tests (headed)...$(NC)"
	npm --prefix $(E2E_DIR) run test:headed

e2e-test-ui: ## Open Playwright interactive UI
	@echo "$(GREEN)Opening Playwright UI...$(NC)"
	npm --prefix $(E2E_DIR) run test:ui

e2e-test-ci: ## Run E2E tests in CI mode
	@echo "$(GREEN)Running E2E tests (CI)...$(NC)"
	npm --prefix $(E2E_DIR) run test:ci

e2e-report: ## Show Playwright HTML report
	@echo "$(GREEN)Opening Playwright report...$(NC)"
	npm --prefix $(E2E_DIR) run test:report

# Combined test targets
test-api: api-test ## Alias for api-test

test-e2e: e2e-test ## Alias for e2e-test

test-frontend: frontend-lint frontend-type-check ## Lint + type-check frontend

test-full: test test-api test-e2e ## Run all tests: backend unit + API + E2E