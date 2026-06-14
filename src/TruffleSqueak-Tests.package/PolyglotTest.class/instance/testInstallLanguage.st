testing
testInstallLanguage
	| currentVersion |
	currentVersion := (Polyglot languageInfoFor: Polyglot languageIdSmalltalk) version asString.
	Polyglot installLanguage: 'js' version: currentVersion path: Smalltalk imagePath