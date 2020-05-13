accessing
exportVariablesFromInput: aText

	| newExportVariables |
	newExportVariables := (aText asString findBetweenSubStrs: ', ').
	newExportVariables ~= self exportVariables ifTrue: [self exportVariables: newExportVariables]