events-processing
handleKeystroke: anEvent 

	(anEvent commandKeyPressed and: [anEvent keyCharacter = $w]) ifTrue: [
			self flag: #todo "close current tab and edit area"].
	
	super handleKeystroke: anEvent