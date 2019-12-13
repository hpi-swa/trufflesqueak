converting
asDate
	(Interop isDate: self)
		ifTrue: [ | javaLocalDate year month day |
			javaLocalDate := Interop asDate: self.
			year := javaLocalDate getYear.
			month := javaLocalDate getMonthValue.
			day := javaLocalDate getDayOfMonth.
			^ Date year: year month: month day: day ]
		ifFalse: [ self error: 'Foreign object cannot be represented as date' ]