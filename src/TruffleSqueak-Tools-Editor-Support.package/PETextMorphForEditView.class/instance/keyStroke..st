embedded morphs
keyStroke: evt

	onKeyStrokeSelector ifNotNil: [ | wasHandled |
		wasHandled := self editView model perform: onKeyStrokeSelector with: evt.
		wasHandled ifTrue: [^ self]].
	
	^super keyStroke: evt