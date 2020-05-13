helpers
error: error withLanguage: language

	self error: language, ': ', (error copyFrom: 'Error: ' size + 1 to: error size)