polyglot
codeForAssignmentOf: value to: name

	"Use var instead of let until running in an inner context is possible. Otherwise we can only execute the code once, because defining the same variable multiple times using let isn't allowed (it is with var)."
	^ 'var ', name, ' = ', value