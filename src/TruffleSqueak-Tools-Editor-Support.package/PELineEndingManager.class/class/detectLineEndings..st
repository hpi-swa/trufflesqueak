detecting
detectLineEndings: aStringOrText

	| counts zeros |
	counts := Dictionary newFrom: {
		String lf -> 0.
		String cr -> 0.
		String crlf -> 0
	}.
	aStringOrText asString lineIndicesDo: [:start :end :endWithLineEnding | | ending |
		end = endWithLineEnding ifFalse: [
			ending := aStringOrText copyFrom: end + 1 to: endWithLineEnding.
			counts at: ending put: (counts at: ending) + 1]].
	zeros := counts values occurrencesOf: 0.
	zeros < 2 ifTrue: [^ #multiple].
	zeros = 3 ifTrue: [^ #unknown].
	^ self stringToId: (counts associations detect: [:each | each value > 0]) key