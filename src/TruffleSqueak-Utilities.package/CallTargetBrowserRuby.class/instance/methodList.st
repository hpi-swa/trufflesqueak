methods
methodList
	| methods countsToMessages sortedCounts highestCount result |
	methods := self selectedMethods.
	(self sortBy == #alphabetically or: [ methods isEmpty])
		ifTrue: [ ^ methods ].
	countsToMessages := self countsToMethods: methods.
	result := OrderedCollection new.
	sortedCounts := countsToMessages keysInOrder reversed.
	highestCount := sortedCounts first max: 1.
	sortedCounts do: [ :count |
		result addAll: ((countsToMessages at: count) collect: [ :ea |
			ea asText
				addAttribute: (CallTargetBrowser textColorFor: count with: highestCount)
				yourself])].
	^ result