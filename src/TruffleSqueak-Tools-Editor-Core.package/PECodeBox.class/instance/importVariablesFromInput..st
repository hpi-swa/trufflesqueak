accessing
importVariablesFromInput: aText

	| newImportVariables |
	newImportVariables := (aText asString findBetweenSubStrs: ', ').
	newImportVariables ~= self importVariables ifTrue: [self importVariables: newImportVariables]