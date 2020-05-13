as yet unclassified
useDuring: aBlock

	"Use myself as UIManager for the duration of aBlock. For the duration, set myself as active via Active and >>isActiveManager (used by UIManager getDefault). Also set the Project's cached UIManger instance to nil so that I am picked up."
	| currentUIManager |
	Active := true.
	currentUIManager := Project uiManager.
	Project current instVarNamed: 'uiManager' put: nil.
	^ [aBlock cull: UIManager default] ensure: [
		Active := false.
		Project current instVarNamed: 'uiManager' put: currentUIManager].