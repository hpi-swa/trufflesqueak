accessing
assertSorted: aCollection name: aName
	| previousElement |
	previousElement := aCollection at: 1.
	2 to: aCollection size do: [ :index |
		| element |
		element := aCollection at: index.
		self assert: (previousElement caseInsensitiveLessOrEqual: element)
			description: aName, ' is not sorted at ', index, '(', previousElement, ' > ', element, ')'.
		previousElement := element ]