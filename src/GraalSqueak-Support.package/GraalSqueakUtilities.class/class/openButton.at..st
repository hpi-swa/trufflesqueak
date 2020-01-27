helpers
openButton: anExpression at: aPosition
	| ws editor |
	ws := Workspace open.
	ws model contents: anExpression.
	editor := (ws findDeepSubmorphThat: [:m | m respondsTo: #editor ] ifAbsent: nil) editor.
	editor selectAll.
	editor doItButtonFromSelection ifNotNil: [:e | e openInWorld. e position: aPosition].
	ws delete