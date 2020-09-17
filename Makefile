calc-res-time-variation:
		mkdir -p dumps/timeVariation
		java src/Search/ConcurrentSearch.java \
				-P \
				-W 0 \
				-R 20 \
				Data/shakespeare.txt "Something is rotten in the state of Denmark." \
				1 \
				> dumps/timeVariation/time_variation.txt
			
run-without-jit:
		mkdir -p dumps/noJit
		java -Xint src/Search/ConcurrentSearch.java \
				-P \
				-W 0 \
				-R 10 \
				Data/shakespeare.txt "Something is rotten in the state of Denmark." \
				1 \
				> dumps/noJit/no_jit.txt

calc-res-time-multiple-tasks:
		mkdir -p dumps/multipleTaskSingle
		for number in 5 10 50 100 ; do \
						java src/Search/ConcurrentSearch.java \
								-P \
								-W 5 \
								-R 10 \
								Data/shakespeare.txt "Something is rotten in the state of Denmark." \
								$$number \
								> dumps/multipleTaskSingle/one_thread_tasks_$$number.txt ; \
		done

calc-res-time-multiple-tasks-cached-thread-pool:
		mkdir -p dumps/cachedThreadTest
		for number in 1 4 6 8 10 12 14 16 18 20 25 40 60 100 ; do \
						java src/Search/ConcurrentSearch.java \
								-P \
								-W 5 \
								-R 10 \
								Data/shakespeare.txt "Something is rotten in the state of Denmark." \
								$$number \
								1 \
								2 \
								> dumps/cachedThreadTest/one_thread_tasks_$$number.txt ; \
		done

calc-res-time-2-tasks:
		mkdir -p dumps/fixedThreadPoolTest
		for number in 1 2 6 12 ; do \
						java src/Search/ConcurrentSearch.java \
								-P \
								-W 5 \
								-R 10 \
								Data/shakespeare.txt "Something is rotten in the state of Denmark." \
								2 \
								$$number \
								1 \
								> dumps/fixedThreadPoolTest/two_tasks_thread_$$number.txt ; \
		done

calc-res-time-10-tasks:
		mkdir -p dumps/fixedThreadPoolTest
		for number in 5 10 20 30 ; do \
						java src/Search/ConcurrentSearch.java \
								-P \
								-W 5 \
								-R 10 \
								Data/shakespeare.txt "Something is rotten in the state of Denmark." \
								10 \
								$$number \
								1 \
								> dumps/fixedThreadPoolTest/ten_tasks_thread_$$number.txt ; \
		done

calc-res-time-30-tasks:
		mkdir -p dumps/fixedThreadPoolTest
		for number in 8 12 30 50 ; do \
						java src/Search/ConcurrentSearch.java \
								-P \
								-W 5 \
								-R 10 \
								Data/shakespeare.txt "Something is rotten in the state of Denmark." \
								30 \
								$$number \
								1 \
								> dumps/fixedThreadPoolTest/thirty_tasks_thread_$$number.txt ; \
		done

test-splitting:
		mkdir -p dumps/testSplitting
				java src/Search/ConcurrentSearch.java \
						-P \
						-W 5 \
						-R 10 \
						Data/xtest.txt "xxxx" \
						5 \
						> dumps/testSplitting/test_splitting.txt	

run-all:
	@$(MAKE) calc-res-time-variation
	@$(MAKE) run-without-jit
	@$(MAKE) calc-res-time-multiple-tasks
	@$(MAKE) calc-res-time-multiple-tasks-cached-thread-pool
	@$(MAKE) calc-res-time-2-tasks
	@$(MAKE) calc-res-time-10-tasks
	@$(MAKE) calc-res-time-30-tasks
	@$(MAKE) test-splitting

delete-all:
	rm -rf dumps

copy-results:
	mkdir -p results
	cp -r dumps/* results/