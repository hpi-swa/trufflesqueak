private
unescapedCommentContent: aString eachCommentLineStartingWith: anotherString

	| endOfComment stop content |
	content := aString.
	stop := false.
	aString lineIndicesDo: [:start :end :endWithLineEnding | | line |
		stop ifFalse: [
			line := aString copyFrom: start to: end.
			"We dont't check for the comment begin marker within the first line since we know we are already in a comment"
			start = 1
				ifTrue: [content := line]
			 	ifFalse: [
					(line beginsWith: anotherString)
						ifTrue: [
							endOfComment := end.
							content := content, (line copyFrom: anotherString size + 1 to: line size)]
						ifFalse: [stop := true]]]].
	
	^ content