serialization
asJson

	self isMarkdown
		ifTrue: [^ self asMarkdownJson]
		ifFalse: [^ self asCodeJson]