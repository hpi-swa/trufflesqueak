message list
messageList
	| messages countsToMessages sortedCounts highestCount result |
	messages := super messageList.
	(self sortBy == #alphabetically or: [ messages isEmpty])
		ifTrue: [ ^ messages ].
	countsToMessages := self countsToMessages: messages.
	result := OrderedCollection new.
	sortedCounts := countsToMessages keysInOrder reversed.
	highestCount := sortedCounts first max: 1.0.
	sortedCounts do: [ :count |
		result addAll: ((countsToMessages at: count) collect: [ :ea |
			ea asText
				addAttribute: (self class textColorFor: count with: highestCount)
				yourself])].
	^ result