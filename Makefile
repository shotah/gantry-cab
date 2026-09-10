# gantry-cab — common operator commands
# Usage: make <target>

GRADLE      ?= ./gradlew
COVERAGE_XML ?= app/build/reports/coverage/test/debug/report.xml
COVERAGE_SVG ?= badges/coverage.svg
COVERAGE_MIN ?= 70

# Release bump: patch (default), minor, or major. Or set TAG=v0.2.0 explicitly.
BUMP ?= patch

.PHONY: help
help: ## Show available targets
	@echo
	@echo gantry-cab targets:
	@echo "  make build          Debug APK (assembleDebug)"
	@echo "  make apk            Sideload APK (assembleRelease)"
	@echo "  make test           Script tests + JVM unit tests"
	@echo "  make test-scripts   Semver + badge + release + hooks (no Android SDK)"
	@echo "  make test-app       ./gradlew testDebugUnitTest"
	@echo "  make lint           Android lint (lintDebug)"
	@echo "  make coverage       JaCoCo XML + 70% mailbox-wire bar"
	@echo "  make check          lint + test + coverage bar"
	@echo "  make ci             lint, test, coverage, debug APK"
	@echo "  make install-hooks  Pre-commit: lint + test + 70% coverage"
	@echo "  make version        Show VERSION + next tag (dry-run)"
	@echo "  make release        Bump tag + latest, update VERSION, push"
	@echo "  make clean          Remove build artifacts"
	@echo
	@echo "SDK: Android Studio writes sdk.dir in local.properties, or set ANDROID_HOME."
	@echo "Release: make release BUMP=patch|minor|major   or   make release TAG=v0.2.0"
	@echo

.PHONY: all
all: check build ## Lint, test, then debug APK

.PHONY: build
build: ## Build the debug APK
	$(GRADLE) assembleDebug
	@echo built app/build/outputs/apk/debug/

.PHONY: apk
apk: ## Build a sideload APK (release build, debug-signed unless a Play key is set)
	$(GRADLE) assembleRelease
	@echo built app/build/outputs/apk/release/

.PHONY: test-scripts
test-scripts: ## Semver + coverage-badge tests (no Android SDK)
	./test/scripts/semver.test.sh
	./test/scripts/coverage-badge.test.sh
	./test/scripts/release.test.sh
	./test/scripts/hooks.test.sh

.PHONY: test-app
test-app: ## JVM unit tests
	$(GRADLE) testDebugUnitTest

.PHONY: test
test: test-scripts test-app ## Script tests + JVM unit tests

.PHONY: lint
lint: ## Android lint
	$(GRADLE) lintDebug

.PHONY: coverage-gate
coverage-gate: ## Fail if mailbox-wire line coverage is below COVERAGE_MIN (70)
	@xml="$(COVERAGE_XML)"; \
	if [ ! -f "$$xml" ]; then \
	  xml=$$(find app/build/reports -name 'report.xml' 2>/dev/null | head -n1); \
	fi; \
	test -n "$$xml" -a -f "$$xml" || (echo "missing JaCoCo report; run make coverage" >&2; exit 1); \
	COVERAGE_MIN="$(COVERAGE_MIN)" ./scripts/coverage-gate.sh "$$xml"

.PHONY: coverage
coverage: ## JaCoCo XML + 70% bar (mailbox wire: Wire + MailboxUrl)
	$(GRADLE) createDebugUnitTestCoverageReport
	@$(MAKE) coverage-gate

.PHONY: coverage-badge
coverage-badge: coverage ## Write badges/coverage.svg from the JaCoCo report
	mkdir -p badges
	./scripts/coverage-badge.sh "$(COVERAGE_XML)" "$(COVERAGE_SVG)"

.PHONY: check
check: test-scripts lint test-app coverage ## Lint, test, 70% coverage (matches pre-commit)

.PHONY: ci
ci: check build ## Local stand-in for CI checks

.PHONY: install-hooks
install-hooks: ## Install git pre-commit hook (lint + test + 70% coverage)
	@top=$$(git rev-parse --show-toplevel 2>/dev/null); \
	here=$$(cd "$(dir $(abspath $(lastword $(MAKEFILE_LIST))))" && pwd); \
	if [ -z "$$top" ] || [ "$$top" != "$$here" ]; then \
	  echo "init this folder as its own git checkout first (like repos/ai-gantry)" >&2; \
	  echo "git root is $${top:-"(none)"}" >&2; \
	  exit 1; \
	fi
	cp scripts/pre-commit .git/hooks/pre-commit
	chmod +x .git/hooks/pre-commit
	@echo "Installed .git/hooks/pre-commit"

.PHONY: version
version: ## Show VERSION file and next tag (dry-run)
	@echo "VERSION $$(tr -d '[:space:]' < VERSION)"
	@DRY_RUN=1 ./scripts/release.sh

# Bump semver, commit VERSION, annotated-tag (v* + floating latest), push
# (triggers GitHub Release + APK). Examples:
#   make release
#   make release BUMP=minor
#   make release BUMP=major
#   make release TAG=v0.2.0
#   make release DRY_RUN=1
.PHONY: release
release: ## Bump version + latest tags, update VERSION, push (BUMP=patch|minor|major)
	BUMP="$(BUMP)" TAG="$(TAG)" DRY_RUN="$(DRY_RUN)" SKIP_PUSH="$(SKIP_PUSH)" ALLOW_DIRTY="$(ALLOW_DIRTY)" ./scripts/release.sh

.PHONY: clean
clean: ## Remove build artifacts
	$(GRADLE) clean
	rm -rf badges

.DEFAULT_GOAL := help
