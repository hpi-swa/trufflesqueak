printing
printOnTitle
	(self hasMembers and: [ self hasArrayElements ]) ifTrue: [ ^ 'ForeignObject[arraySize=', self basicSize, ', memberSize=', self instSize, ']' ].
	self hasArrayElements ifTrue: [ ^ 'ForeignObject[arraySize=', self basicSize, ']' ].
	self hasMembers ifTrue: [ ^ 'ForeignObject[memberSize=', self instSize, ']' ].
	^ 'ForeignObject'