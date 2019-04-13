events-processing
handleMouseDown: anEvent
	| menu action |
	menu := CustomMenu new.
	menu add: PolyglotNotebook markdownName action: PolyglotNotebook markdownID.
	menu addLine.
	
	Polyglot availableLanguages do: [:ea |
		menu add: (Polyglot languageNameOf: ea) action: ea.
		].
	menu addLine.
	menu add: 'run cell...' action: #run.
	menu addLine.
	menu add: 'up...' action: #up.
	menu add: 'down...' action: #down.
	menu addLine.
	menu add: 'remove...' action: #remove.
	anEvent wasHandled: true.
	action := menu startUp: PolyglotNotebook markdownID.
	action = #run ifTrue: [ self codeCell run. ^ self ].
	action = #up ifTrue: [ self codeCell up. ^ self ].
	action = #down ifTrue: [ self codeCell down. ^ self ].
	action = #remove ifTrue: [ self codeCell remove. ^ self ].
	self codeCell languageID: action.
	self refresh.
	