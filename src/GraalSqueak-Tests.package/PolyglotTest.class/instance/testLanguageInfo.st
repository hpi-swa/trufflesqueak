testing
testLanguageInfo
	self assert: (Polyglot languageDefaultMimeTypeOf: Polyglot languageIdJS) notEmpty.
	self assert: (Polyglot languageMimeTypesOf: Polyglot languageIdJS) notEmpty.
	self assert: (Polyglot languageNameOf: Polyglot languageIdJS) notEmpty.
	self assert: (Polyglot languageVersionOf: Polyglot languageIdJS) notEmpty.