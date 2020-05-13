private
unstyledTextFrom: aText
	"Re-implemented so that TextAnchors are not removed from aText"
	| answer |	
	answer := super unstyledTextFrom: aText.
	"answer := answer copyReplaceAll: (String value: 1) with: ''."
	aText runs withStartStopAndValueDo: [:start :stop :attributes | attributes
		select: [:each | each isKindOf: TextAnchor] 
		thenDo: [:each | answer addAttribute: each from: start to: stop]].
	^answer