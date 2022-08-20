"Remove active context."
Processor activeProcess instVarNamed: #suspendedContext put: nil.

"Modify StartUpList for headless execution."
{EventSensor. Project} do: [:ea | Smalltalk removeFromStartUpList: ea].

"Start up image (see SmalltalkImage>>#snapshot:andQuit:withExitCode:embedded:)."
Smalltalk
	clearExternalObjects;
	processStartUpList: true;
	setPlatformPreferences;
	recordStartupStamp.

"Set author information."
Utilities
	authorName: 'TruffleSqueak';
	setAuthorInitials: 'TruffleSqueak'.

"Initialize fresh MorphicUIManager."
Project current instVarNamed: #uiManager put: MorphicUIManager new.
