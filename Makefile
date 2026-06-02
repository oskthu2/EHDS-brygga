.PHONY: up down build test e2e validate ig logs

up:
	docker compose up -d

down:
	docker compose down

build:
	docker compose build

test: up
	bash scripts/test.sh

e2e: up
	@echo "Waiting 5s for stack to be ready..."; sleep 5
	bash scripts/e2e-test.sh

validate:
	bash scripts/validate.sh

ig:
	bash scripts/build-ig.sh

logs:
	docker compose logs -f

restart:
	docker compose down && docker compose up -d --build
