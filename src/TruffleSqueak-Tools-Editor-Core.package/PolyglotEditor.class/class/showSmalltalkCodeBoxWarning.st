preferences
showSmalltalkCodeBoxWarning

	UIManager default inform: 'When using Smalltalk code boxes, it is your responsibility to predeclare all variables the code box _exports_ at the top of the enclosing file using | anExportedVariable |. You also must end the code inside the code box with a dot ".". This warning will only be shown once.'.
	SmalltalkCodeBoxWarningShown := true.