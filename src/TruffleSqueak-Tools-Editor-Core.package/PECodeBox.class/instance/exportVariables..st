accessing
exportVariables: anObject

	exportVariables := anObject.
	self changed: #exportVariablesForInput.
	self triggerEvent: #codeBoxChanged