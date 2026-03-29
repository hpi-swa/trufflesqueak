language
setAdaptiveLanguageSelection
	Polyglot askForAdaptiveLanguageSelection.
	(Polyglot useAdaptiveLanguageSelection and: [
			Interop hasLanguageId: self object ]) ifTrue: [
		self languageInfo: (Interop getLanguageInfo: self object ) ]