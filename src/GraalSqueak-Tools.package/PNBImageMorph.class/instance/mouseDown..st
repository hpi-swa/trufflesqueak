event handling
mouseDown: anEvent
	| menu action |
	menu := CustomMenu new.
	menu add: 'copy...' action: #copy.
	menu add: 'inspect...' action: #inspect.
	anEvent wasHandled: true.
	action := menu startUp: #copy.
	action = #copy ifTrue: [ self form copy asMorph openInHand. ^ self ].
	action = #inspect ifTrue: [ self inspect. ^ self ].