drawing
drawOn: aCanvas

	| lineNumber lineStartAndEnds lastLineEndsWithLineEnding defaultColor |
	super drawOn: aCanvas.
	
	aCanvas line: self lineNumberWidth@0 to: (self lineNumberWidth@self height) color: Color black.
	
	lineStartAndEnds := OrderedCollection new.
	lastLineEndsWithLineEnding := true.
	text asString lineIndicesDo: [:start :end :endWithLineEnding | 
		lineStartAndEnds add: (start to: endWithLineEnding).
		lastLineEndsWithLineEnding := end < endWithLineEnding].
	lastLineEndsWithLineEnding ifTrue: [lineStartAndEnds add: (text size + 1 to: text size + 1)].
	
	defaultColor := self color brightness < 0.6 ifTrue: [self color muchLighter] ifFalse: [self color muchDarker].
	
	lineNumber := 1.
	paragraph lines do: [:each | | lineStartAndEnd |
		lineStartAndEnd := lineStartAndEnds at: lineNumber ifAbsent: [Float infinity to: Float infinity].
		(each first >= lineStartAndEnd start) ifTrue: [
			aCanvas 
				drawString: lineNumber asString 
				at: (self margins left - self lineNumberWidth)@(each top + 1)
				font: textStyle defaultFont
				color: defaultColor.
			lineNumber := lineNumber + 1]]
	