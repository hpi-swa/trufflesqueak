accessing
autoHeight: aBoolean

	autoHeight := aBoolean.
	autoHeight 
		ifTrue: [	"self vScrollBarPolicy: #never" self flag: #todo "removing the scrollbar breaks autoHeight"]
		ifFalse: [self vScrollBarPolicy: #whenNeeded]