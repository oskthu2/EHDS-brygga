.PHONY: up down build test validate ig logs

up:
	docker compose up -d

down:
	docker compose down

build:
	docker compose build

test: up
	bash scripts/test.sh

validate:
	bash scripts/validate.sh

ig:
	bash scripts/build-ig.sh

logs:
	docker compose logs -f

restart:
	docker compose down && docker compose up -d --build
