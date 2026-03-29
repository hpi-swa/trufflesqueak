language
updateAdaptiveLanguageSelection
	(Polyglot useAdaptiveLanguageSelection and: [
			Interop hasLanguageId: self object ]) ifTrue: [
		self languageInfo: (Interop getLanguageInfo: self object ) ]