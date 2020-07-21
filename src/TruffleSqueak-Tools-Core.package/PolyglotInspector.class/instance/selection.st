selecting
selection
	"The receiver has a list of variables of its inspected object.
	One of these is selected. Answer the value of the selected variable."
	| basicIndex |
	selectionIndex = 0 ifTrue: [^ ''].
	selectionIndex = 1 ifTrue: [^ object].
	selectionIndex = (1 + (self optionalFieldList indexOf: 'members')) ifTrue: [^ self longPrintStringFor: object].
	selectionIndex = (1 + (self optionalFieldList indexOf: 'source')) ifTrue: [^ self getSource: object].
	basicIndex := selectionIndex - 1 - self optionalFieldList size.
	(object basicSize <= (self i1 + self i2) or: [basicIndex <= self i1])
		ifTrue: [^ object basicAt: basicIndex]
		ifFalse: [^ object basicAt: object basicSize - (self i1 + self i2) + basicIndex]