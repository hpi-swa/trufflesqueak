testing
testCannotInterpretFallback
	| trapSuperclass trapClass instance result |
    
	"Create a superclass that explicitly handles #cannotInterpret:"
	trapSuperclass := Behavior new.
	trapSuperclass superclass: Object.
	trapSuperclass setFormat: Object format.
	trapSuperclass methodDictionary: MethodDictionary new.
    
	"Compile a custom #cannotInterpret: that just returns the failing Message"
	trapSuperclass compile: 'cannotInterpret: aMessage
		^ aMessage'.

	"Create our broken anonymous class, inheriting from our custom superclass"
	trapClass := Behavior new.
	trapClass superclass: trapSuperclass. 
	trapClass setFormat: Object format.
	trapClass methodDictionary: nil. 

	"Instantiate the broken class"
	instance := trapClass basicNew.

	"Send a message to the instance of the broken class."
	result := instance thisMessageWillTriggerCannotInterpret.
	self assert: result class == Message.
	self assert: result selector == #thisMessageWillTriggerCannotInterpret.
	
	result := instance inject: 1 into: [ :a :b | a + b].
	self assert: result class == Message.
	self assert: result selector == #inject:into:.
	self assert: result arguments size = 2.
	self assert: result arguments first = 1.
