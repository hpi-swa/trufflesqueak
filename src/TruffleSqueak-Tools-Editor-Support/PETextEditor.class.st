"
I am a text editor that allows you to set the lineEndings to use when hitting CR.
"
Class {
	#name : #PETextEditor,
	#superclass : #SmalltalkEditor,
	#instVars : [
		'lineEndings'
	],
	#category : 'TruffleSqueak-Tools-Editor-Support'
}

{ #category : #'keyboard shortcut tables' }
PETextEditor class >> initialize [
	"PCTextEditor initialize"
	super initialize
]

{ #category : #'typing/selecting keys' }
PETextEditor >> backspace: aKeyboardEvent [ 
	"Backspace over the last character."

	| startIndex charactersToRemove |
	aKeyboardEvent shiftPressed ifTrue: [^ self backWord: aKeyboardEvent].
	startIndex := self markIndex +
				(self hasCaret ifTrue: [0] ifFalse: [1]).
	
	(self isLineEndingBefore: startIndex) 
		ifTrue: [charactersToRemove := self lineEndings size]
		ifFalse: [charactersToRemove := 1].
	startIndex := 1 max: startIndex - charactersToRemove.

	^ self backTo: startIndex
]

{ #category : #private }
PETextEditor >> clipboardText [

	"Ensure correct line endings when pasting from clipboard"
	^ PELineEndingManager with: self lineEndings string: Clipboard clipboardText
]

{ #category : #'typing/selecting keys' }
PETextEditor >> cursorLeft: aKeyboardEvent [

	self insertAndCloseTypeIn.
	self
		moveCursor:[:position | ((self isLineEndingBefore: position)
			ifTrue: [position - self lineEndings size]
			ifFalse: [position - 1]) max: 1]
		forward: false
		event: aKeyboardEvent
		specialBlock:[:position | self previousWord: position].
	^ true
]

{ #category : #'typing/selecting keys' }
PETextEditor >> cursorRight: aKeyboardEvent [

	self insertAndCloseTypeIn.
	self
		moveCursor:[:position | (self isLineEndingAt: position)
			ifTrue: [position + self lineEndings size]
			ifFalse: [position + 1]]
		forward: true
		event: aKeyboardEvent
		specialBlock:[:position | self nextWord: position].
	^ true
]

{ #category : #'typing support' }
PETextEditor >> dispatchOnKeyboardEvent: aKeyboardEvent [ 

	aKeyboardEvent keyCharacter == Character cr ifTrue: [
		self clearParens.
		self addString: (String streamContents: [:characterStream | 
			characterStream
				nextPutAll: self lineEndings;
				tab: self tabCount]).
		^ false].
	
	^ super dispatchOnKeyboardEvent: aKeyboardEvent
]

{ #category : #'typing/selecting keys' }
PETextEditor >> forwardDelete: aKeyboardEvent [
	"Delete forward over the next character.
	  Make Undo work on the whole type-in, not just the one char.
	wod 11/3/1998: If there was a selection use #zapSelectionWith: rather than #backspace: which was 'one off' in deleting the selection. Handling of things like undo or typeIn area were not fully considered."

	| startIndex stopIndex |
	morph readOnly ifTrue: [^ self].
	
	self hasSelection ifTrue: [
		"Create checkpoint in history."
		self replaceSelectionWith: self nullText.
		^ true].

	startIndex := self markIndex.
	startIndex > self text size ifTrue: [^ false].

	stopIndex := (self isLineEndingAt: startIndex)
		ifTrue: [startIndex + self lineEndings size - 1]
		ifFalse: [startIndex].

	"Forward delete next word"
	self flag: #consistency. "mt: We might want to implemented it like #backspace: and #backWord:."
	aKeyboardEvent shiftPressed
		ifTrue: [stopIndex := (self firstWordBoundaryAfter: stopIndex) - 1].

	self selectInvisiblyFrom: startIndex to: stopIndex.
	
	self isTypingIn ifTrue: [
		self history current type = #forwardDelete
			ifFalse: [self closeTypeIn]
			ifTrue: [
				"Append next characters that will be removed."
				self history current contentsBefore append: self selection.
				self history current intervalBefore in:  [:i |
					self history current intervalBefore: (i first to: i last + (stopIndex - startIndex + 1))]]].
	
	self openTypeInFor: #forwardDelete.
	self zapSelectionWith: self nullText.
	
	^ false
]

{ #category : #'initialize-release' }
PETextEditor >> initialize [

	super initialize.
	self lineEndings: String lf
]

{ #category : #'typing/selecting keys' }
PETextEditor >> isLineEndingAt: aNumber [

	^ (aNumber <= self text size and: [self text at: aNumber]) = self lineEndings first
]

{ #category : #'typing/selecting keys' }
PETextEditor >> isLineEndingBefore: aNumber [

	^ (aNumber > 1 and: [self text at: aNumber - 1]) = self lineEndings last
]

{ #category : #accessing }
PETextEditor >> lineEndings [

	^ lineEndings
]

{ #category : #accessing }
PETextEditor >> lineEndings: aString [

	lineEndings := aString
]

{ #category : #private }
PETextEditor >> sameColumn: start newLine: lineBlock forward: isForward [
	"Private - Compute the index in my text
	with the line number derived from lineBlock,"
	" a one argument block accepting the old line number.
	The position inside the line will be preserved as good as possible"
	"The boolean isForward is used in the border case to determine if
	we should move to the beginning or the end of the line."
	| wordStyle column currentLine offsetAtTargetLine targetEOL lines numberOfLines currentLineNumber targetLineNumber |
	wordStyle := Preferences wordStyleCursorMovement.
	wordStyle
		ifTrue: [
			lines := paragraph lines.
			numberOfLines := paragraph numberOfLines.
			currentLineNumber  := paragraph lineIndexOfCharacterIndex: start.
			currentLine := lines at: currentLineNumber]
		ifFalse: [
			lines := self lines.
			numberOfLines := lines size.
			currentLine := lines
				detect:[:lineInterval | lineInterval last >= start]
				ifNone:[lines last].
			currentLineNumber := currentLine second].
	column := start - currentLine first.
	targetLineNumber := ((lineBlock value: currentLineNumber) max: 1) min: numberOfLines.
	offsetAtTargetLine := (lines at: targetLineNumber) first.
	targetEOL := (lines at: targetLineNumber) last + (targetLineNumber = numberOfLines ifTrue:[1]ifFalse:[0]). 
	
	(targetEOL <= self text size and: [self text at: targetEOL]) = self lineEndings last ifTrue: [
		targetEOL := targetEOL - self lineEndings size + 1].
	
	targetLineNumber = currentLineNumber
	"No movement or movement failed. Move to beginning or end of line."
		ifTrue:[^isForward
			ifTrue:[targetEOL]
			ifFalse:[offsetAtTargetLine]].
	^offsetAtTargetLine + column min: targetEOL.
]
