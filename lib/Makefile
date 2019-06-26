TARGET = SqueakFFIPrims

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
	LIBFLAGS = -shared
	EXTENSION = so
endif
ifeq ($(UNAME_S),Darwin)
	LIBFLAGS = -dynamiclib
	EXTENSION = dylib
endif

$(TARGET).$(EXTENSION): $(TARGET).c
	$(CC) -fPIC $(LIBFLAGS) $(TARGET).c -o $(TARGET).$(EXTENSION)

.PHONY: clean
clean:
	rm -f $(TARGET).$(EXTENSION)
