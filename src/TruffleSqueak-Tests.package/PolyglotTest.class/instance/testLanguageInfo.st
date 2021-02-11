testing
testLanguageInfo
	| info |
	info := Polyglot languageInfoFor: Polyglot languageIdSmalltalk.
	self assert: info getId equals: Polyglot languageIdSmalltalk.
	self assert: info getName isString.
	self assert: info getVersion isString.
	self assert: info getDefaultMimeType isString.
	self assert: info getMimeTypes notNil.
	self assert: info isInteractive.
	self deny: info isInternal.