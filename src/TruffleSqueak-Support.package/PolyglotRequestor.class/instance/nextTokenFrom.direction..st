requests
nextTokenFrom: start direction: dir
	"simple token-finder for compiler automated corrections"
	| loc |
	loc := start + dir.
	[(loc between: 1 and: currentSource size) and: [(currentSource at: loc) isSeparator]]
		whileTrue: [loc := loc + dir].
	^ loc