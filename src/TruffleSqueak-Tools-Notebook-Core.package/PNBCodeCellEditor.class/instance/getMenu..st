event handling
getMenu: shiftKeyState 
	| menu |
	menu := (super getMenu: shiftKeyState) copy.
	menu
		addLine;
		add: 'set syntax highlighter... (h)'
			target: self
			selector: #setSyntaxHighlighter.
	^ menu