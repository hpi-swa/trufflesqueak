testing
testMutatingSuperSend
	| superA superB subClass instance result |

	"1. Create the Old Superclass (returns 1)"
	superA := Object subclass: #TSVMSuperSendTestA 
		instanceVariableNames: '' 
		classVariableNames: '' 
		poolDictionaries: '' 
		category: 'TruffleSqueak-Tests'.
	superA compile: 'dummy: a with: b with: c ^ 1'.

	"2. Create the New Superclass (returns 2)"
	superB := Object subclass: #TSVMSuperSendTestB
		instanceVariableNames: '' 
		classVariableNames: '' 
		poolDictionaries: '' 
		category: 'TruffleSqueak-Tests'.
	superB compile: 'dummy: a with: b with: c ^ 2'.

	"3. Create our Target Class, initially inheriting from SuperA"
	subClass := superA subclass: #TSVMSuperSendTestSub 
		instanceVariableNames: '' 
		classVariableNames: '' 
		poolDictionaries: '' 
		category: 'TruffleSqueak-Tests'.
		
	[
		"4. Install a method that changes its own superclass mid-execution"
		subClass compile: 'doMutatingSuperSend
			"Redefine the class to inherit from SuperB. This triggers a becomeForward:"
			TSVMSuperSendTestB subclass: #TSVMSuperSendTestSub 
				instanceVariableNames: '''' 
				classVariableNames: '''' 
				poolDictionaries: '''' 
				category: ''TruffleSqueak-Tests''.

			"Execute extended super send. 
			- Unpatched AST will use the stale class and call SuperA >> dummy (returns 1).
			- Patched AST will heal itself, follow the forwarding pointer to the new class, 
			and call SuperB >> dummy (returns 2)."
			^ super dummy: 1 with: 2 with: 3'.

		"5. Execute the test"
		instance := subClass new.
		result := instance doMutatingSuperSend.
	] ensure: [
		"6. Cleanup"
		subClass removeFromSystem.
		superA removeFromSystem.
		superB removeFromSystem.
	].

	"7. Assert the fix"
	self assert: result = 2
