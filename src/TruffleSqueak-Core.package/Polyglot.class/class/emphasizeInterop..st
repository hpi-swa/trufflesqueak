preferences
emphasizeInterop: aBoolean

	EmphasizeInterop := aBoolean.

	"Update all object explorer lists"
	(ObjectExplorer allSubInstances, PolyglotNotebook allInstances)
		do: [ :ea | ea changed: #getList ]