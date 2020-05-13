polyglot
unescapedCommentContent: aString

	| found start endOfComment |
	"Search for the first occurrence of a double quote that is not followed by another double quote."
	found := false.
	start := 1.
	[found] whileFalse: [
		endOfComment := aString indexOf: $" startingAt: start.
		(endOfComment = 0 or: [endOfComment = aString size] or: [(aString at: endOfComment + 1) ~= $"])
			ifTrue: [found := true]
			ifFalse: [start := endOfComment + 2]].
	
	"Copy string up until the double quote (if found) and replace all double double quotes by a single double quote"
	endOfComment = 0 
		ifTrue: [endOfComment := aString size]
		ifFalse: [endOfComment := endOfComment - 1].
	^ (aString copyFrom: 1 to: endOfComment) copyReplaceAll: '""' with: '"'