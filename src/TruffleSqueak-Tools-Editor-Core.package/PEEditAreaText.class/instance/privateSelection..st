accessing
privateSelection: anInterval.

	"This is only used by ToolBuilder. If we were to use >>selection:, we'd cause an infinite loop"
	selection := anInterval