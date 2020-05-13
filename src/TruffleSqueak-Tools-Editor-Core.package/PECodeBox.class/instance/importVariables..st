accessing
importVariables: anObject

	importVariables := anObject.
	self changed: #importVariablesForInput.
	self triggerEvent: #codeBoxChanged