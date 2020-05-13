accessing
code: aText

	| text |
	text := aText asText.
	code := text.
	unsavedCode := text.
	self changed: #code.
	self extractLineEndings