accessing
code: aText

	code := aText.
	self changed: #code.
	self triggerEvent: #codeBoxChanged