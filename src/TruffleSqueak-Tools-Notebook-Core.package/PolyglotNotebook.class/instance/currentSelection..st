model
currentSelection: anObject

	self currentSelection == anObject ifTrue: [^ self].
	
	currentSelection := anObject.
	
	self changed: #currentSelection.