language
setAdaptiveLanguageSelection
	Polyglot askForAdaptiveLanguageSelection.
	(Polyglot useAdaptiveLanguageSelection and: [
			Interop hasLanguage: self object ]) ifTrue: [
		self languageInfo: (Interop getLanguageInfo: self object ) ]