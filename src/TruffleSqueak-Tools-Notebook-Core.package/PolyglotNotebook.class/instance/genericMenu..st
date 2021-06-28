actions
genericMenu: aMenu 
	^ aMenu
		add: 'inspect (i)'
			target: self currentSelection value
			selector: #inspect;
		add: 'explore (I)'
			target: self currentSelection value
			selector: #explore