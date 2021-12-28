FLAGS = -I include

OBJS = \
		src/main.o \

EXEC = view_packet_tool.o

src/%.o: src/%.c
	gcc $(FLAGS) -c $< -o $@ -lpcap

$(EXEC): $(OBJS)
	gcc $(FLAGS) $(OBJS) -o $@ -lpcap

run: $(EXEC)
	./$(EXEC)
