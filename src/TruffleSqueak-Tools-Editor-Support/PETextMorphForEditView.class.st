"
I am a custom text morph that resizes all embedded morphs to my width whenever my width changes or they are added.
"
Class {
	#name : #PETextMorphForEditView,
	#superclass : #TextMorphForEditView,
	#instVars : [
		'onKeyStrokeSelector'
	],
	#category : 'TruffleSqueak-Tools-Editor-Support'
}

{ #category : #drawing }
PETextMorphForEditView >> drawOn: aCanvas [

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
	
]

{ #category : #private }
PETextMorphForEditView >> editorClass [

	^ PETextEditor
]

{ #category : #geometry }
PETextMorphForEditView >> extent: aPoint [

	super extent: aPoint.
	self resizeEmbeddedMorphs
]

{ #category : #'embedded morphs' }
PETextMorphForEditView >> keyStroke: evt [

	onKeyStrokeSelector ifNotNil: [ | wasHandled |
		wasHandled := self editView model perform: onKeyStrokeSelector with: evt.
		wasHandled ifTrue: [^ self]].
	
	^super keyStroke: evt
]

{ #category : #accessing }
PETextMorphForEditView >> lineNumberWidth [

	^ 30
]

{ #category : #accessing }
PETextMorphForEditView >> margins: aRectangle [

	super margins: (aRectangle left: aRectangle left + self lineNumberWidth)
]

{ #category : #accessing }
PETextMorphForEditView >> newContents: stringOrText [

	super newContents: stringOrText.
	self resizeEmbeddedMorphs
]

{ #category : #'embedded morphs' }
PETextMorphForEditView >> resizeEmbeddedMorphs [

	text embeddedMorphs do: [:each |
		each width: self width - (self margins left + self margins right)]
]

{ #category : #accessing }
PETextMorphForEditView >> setOnKeyStrokeSelector: aSymbol [

	onKeyStrokeSelector := aSymbol
]
