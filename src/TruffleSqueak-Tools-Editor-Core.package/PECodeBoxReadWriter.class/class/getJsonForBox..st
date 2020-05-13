writing
getJsonForBox: aPCCodeBox

	^ Json render: (JsonObject newFrom: {
		#boxName -> aPCCodeBox boxName.
		#exportVariables -> aPCCodeBox exportVariables.
		#importVariables -> aPCCodeBox importVariables.
		#language -> aPCCodeBox language})