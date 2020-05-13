accessing
boxName: aText

	boxName := aText asString.
	self changed: #boxName.
	self triggerEvent: #codeBoxChanged