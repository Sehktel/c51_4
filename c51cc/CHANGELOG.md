# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Changed
- Add a new arity to `make-widget-async` to provide a different widget shape.

## [0.1.1] - 2025-05-06
### Parser
- Двойное отрицане и сложные унарные операции не поддерживаются !!x -- это что-то пока недоступное.
- Добавлени инкримент и дикремент 
- исправлена токенизация 

## [0.1.1] - 2025-05-06
### Changed
- Documentation on how to make the widgets.

### Removed
- `make-widget-sync` - we're all async, all the time.

### Fixed
- Fixed widget maker to keep working when daylight savings switches over.

## 0.1.0 - 2025-05-06
### Added
- Files from the new template.
- Widget maker public API - `make-widget-sync`.

[Unreleased]: https://sourcehost.site/your-name/c51cc/compare/0.1.1...HEAD
[0.1.1]: https://sourcehost.site/your-name/c51cc/compare/0.1.0...0.1.1
