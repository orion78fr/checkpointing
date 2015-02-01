CLASSES:= $(shell find src -name '*.java')
.PHONY: all random random_domino rollbackkill rollbackkill_domino clean

all: $(CLASSES)
	javac -classpath src:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:lib/peersim-1.0.5.jar:lib/jgraphx.jar $(CLASSES) -Xlint:unchecked -d bin

random:
	java -classpath bin:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:lib/peersim-1.0.5.jar:lib/jgraphx.jar peersim/Simulator config/config_file_randomKill_dominoFalse.cfg

random_domino:
	java -classpath bin:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:lib/peersim-1.0.5.jar:lib/jgraphx.jar peersim/Simulator config/config_file_randomKill_dominoTrue.cfg

rollbackkill:
	java -classpath bin:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:lib/peersim-1.0.5.jar:lib/jgraphx.jar peersim/Simulator config/config_file_rollbackKill_dominoFalse.cfg

rollbackkill_domino:
	java -classpath bin:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:lib/peersim-1.0.5.jar:lib/jgraphx.jar peersim/Simulator config/config_file_rollbackKill_dominoTrue.cfg

visual_random:
	java -classpath bin:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:lib/peersim-1.0.5.jar:lib/jgraphx.jar araproject/Visualizer config/config_file_randomKill_dominoFalse.cfg

visual_random_domino:
	java -classpath bin:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:lib/peersim-1.0.5.jar:lib/jgraphx.jar araproject/Visualizer config/config_file_randomKill_dominoTrue.cfg

visual_rollbackkill:
	java -classpath bin:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:lib/peersim-1.0.5.jar:lib/jgraphx.jar araproject/Visualizer config/config_file_rollbackKill_dominoFalse.cfg

visual_rollbackkill_domino:
	java -classpath bin:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:lib/peersim-1.0.5.jar:lib/jgraphx.jar araproject/Visualizer config/config_file_rollbackKill_dominoTrue.cfg

clean:
	rm -f `find . -name "*.class"`
	
