language
updateAdaptiveLanguageSelection
	(Polyglot useAdaptiveLanguageSelection and: [
			Interop hasLanguage: self object ]) ifTrue: [
		self languageInfo: (Interop getLanguageInfo: self object ) ]