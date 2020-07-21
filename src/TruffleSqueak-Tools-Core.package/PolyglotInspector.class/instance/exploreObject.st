toolbuilder
exploreObject
	"Switch to an explorer tool."
	
	| window currentBounds |
	currentBounds := ToolBuilder findDefault getBoundsForWindow: self containingWindow.

	"Close first because MVC fiddles around with processes."
	self changed: #close. 
		
	window := PolyglotObjectExplorer openOn: self object.
	
	"---- In MVC, the lines after this will not be executed ---"

	window model setExpression: self expression.
	ToolBuilder findDefault setBoundsForWindow: window to: currentBounds.