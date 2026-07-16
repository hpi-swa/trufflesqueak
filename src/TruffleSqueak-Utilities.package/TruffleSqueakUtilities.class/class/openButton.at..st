helpers
openButton: anExpression at: aPosition
	| ws textMorph editor |
	
	ws := Workspace open.
	ws contents: anExpression.
	
	textMorph := ws codeTextMorph.
	
	editor := (textMorph findDeepSubmorphThat: [ :m | m respondsTo: #editor ] ifAbsent: nil) editor.
	editor selectAll.
	editor doItButtonFromSelection ifNotNil: [ :e |
		e
			changeClassTo: TruffleLauncherButton;
			openInWorld;
			position: aPosition
	].

	textMorph owner delete
