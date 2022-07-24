helpers
openButton: anExpression at: aPosition
	| textMorph ws editor |
	ws := Workspace open.
	ws contents: anExpression.
	textMorph := ws codeTextMorph.
	editor := (textMorph findDeepSubmorphThat: [:m | m respondsTo: #editor ] ifAbsent: nil) editor.
	editor selectAll.
	editor doItButtonFromSelection ifNotNil: [:e | e openInWorld. e position: aPosition].
	textMorph owner delete
