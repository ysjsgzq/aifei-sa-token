# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2026-05-04

### Added

- Initial Aifei integration plugin for `Sa-Token`
- `SaTokenAifeiPlugin` for bootstrapping config, context, and route matcher
- `SaTokenUndertowDispatcher` for binding and clearing `Sa-Token` context in Undertow requests
- Undertow bridge implementations for `SaRequest`, `SaResponse`, and `SaStorage`
- `SaAnnotationInterceptor` for annotation-based authorization
- `SaTokenInterceptor` for route-based authorization with optional annotation checks
- `PathPatternMatcher` for Ant-style route matching
- GitHub Actions build workflow
- Project README with setup instructions and usage examples
- Source and Javadoc JAR packaging in Maven build
