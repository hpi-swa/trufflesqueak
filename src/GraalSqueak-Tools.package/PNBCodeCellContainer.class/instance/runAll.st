actions
runAll
	self submorphs do: [ :cell | cell run ]  displayingProgress: [ :cell | 'Executing cells...' ]