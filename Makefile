.PHONY: build test lint format check clean

build:
	./gradlew build

test:
	./gradlew test

lint:
	./gradlew detekt

format:
	./gradlew spotlessApply

check: format lint test build
	@echo "All checks passed."

clean:
	./gradlew clean
