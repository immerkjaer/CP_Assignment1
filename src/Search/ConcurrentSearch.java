package Search;
// java -Xint Main.java -P -W 0 -R 5 C:\Users\mathi\IdeaProjects\hej\src\Search\lort.txt "Something is rotten in the state of Denmark."
// -P -W 5 -R 5 C:\Users\mathi\IdeaProjects\hej\src\Search\xtest.txt "xxxx" 8 1
// -P -W 5 -R 5 C:\Users\mathi\IdeaProjects\TextSearch\Data\shakespeare.txt "Something is rotten in the state of Denmark." 8 1 "newCachedThreadPool"
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.*;

class Search {

    static final int max = 10000000; // Max no. of chars searched

    static char[] text = new char[max]; // file to be searched
    static int len;                     // Length of actual text
    static String fname;                // Text file name
    static char[] pattern;              // Search pattern
    static int ntasks = 1;              // No. of tasks
    static int nthreads = 1;            // No. of threads to use
    static boolean printPos = false;    // Print all positions found
    static int warmups = 0;             // No. of warmup searches
    static int runs = 1;                // No. of search repetitions
    static String  datafile;            // Name of data file
    static int threadpoolType = 0;


    static void getArguments(String[] argv) {
        // Reads arguments into static variables
        try {
            int i = 0;

            if (argv.length < 2)
                throw new Exception("Too few arguments");

            while (i < argv.length) {

                /* Check for options */
                if (argv[i].equals("-P")) {
                    printPos = true;
                    i++;
                    continue;
                }

                if (argv[i].equals("-R")) {
                    runs = new Integer(argv[i+1]);
                    i += 2;
                    continue;
                }

                if (argv[i].equals("-W")) {
                    warmups = new Integer(argv[i+1]);
                    i += 2;
                    continue;
                }

                if (argv[i].equals("-d")) {
                    datafile = argv[i+1];
                    i += 2;
                    continue;
                }

                /* Handle positional parameters */
                fname = argv[i];
                pattern = argv[i + 1].toCharArray();
                i += 2;

                if (argv.length > i) {
                    ntasks = new Integer(argv[i]);
                    i++;
                }

                if (argv.length > i) {
                    nthreads = new Integer(argv[i]);
                    i++;
                }

                if (argv.length > i) {
                    threadpoolType = new Integer(argv[i]);
                    i++;
                }

                if (argv.length > i)
                    throw new Exception("Too many arguments");
            }

            /* Read file into memory */
            InputStreamReader file = new InputStreamReader(new FileInputStream(fname));

            len = file.read(text);

            if (file.read() >= 0)
                System.out.println("\nWarning: file truncated to " + max + " characters\n");

            if (ntasks <= 0 || nthreads <= 0 || pattern.length <= 0 || warmups <0 || runs <= 0)
                throw new Exception("Illegal argument(s)");

        } catch (Exception e) {
            System.out.print(e + "\n\nUsage:   java Search <options> file pattern [ntasks [nthreads]] \n\n"
                    + "  where: 0 < nthreads, 0 < ntasks, 0 < size(pattern)\n" + "  Options: \n"
                    + "    -P           Print found positions\n"
                    + "    -W w         Make w warmup searches (w >=0)\n"
                    + "    -R r         Run the search n times (r > 0)\n"
                    + "    -d datafile  Define datafile\n\n" );
            System.exit(1);
        }
    }

    static void writeResult(List<Integer> res) {
        System.out.print("" + res.size() + " occurrences found in ");
        if (printPos) {
            int i = 0;
            System.out.println();
            for (int pos : res) {
                System.out.printf(" %6d", pos);
                if (++i % 10 == 0)
                    System.out.println();
            }
            System.out.println();
        }
    }

    static void writeTime(double time) {
        System.out.printf("%1.6f\n", time);
    }

    static void writeRun(int no) {
        System.out.printf("Run no. %2d: ", no);
    }

    static void writeData(String s) {
        try {
            if (datafile != null) {
                // Append result to data file
                FileWriter f = new FileWriter(datafile,true);
                PrintWriter data =  new PrintWriter(new BufferedWriter(f));
                data.println(s);
                data.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) throws InterruptedException {
        try {
            long start;
            double time, totalTime = 0.0;

            ExecutorService engine = Executors.newSingleThreadExecutor();

            /* Get and print program parameters */
            getArguments(argv);


            /* Setup execution engine */
            if (threadpoolType == 1) {
                engine = Executors.newFixedThreadPool(nthreads);
            }
            else if (threadpoolType == 2) {
                engine = Executors.newCachedThreadPool();
            }

            System.out.printf("\nFile=%s, pattern='%s', ntasks=%d, nthreads=%d, warmups=%d, runs=%d, thread pool=%s\n\n",
                    fname, new String(pattern), ntasks, nthreads, warmups, runs, threadpoolType);

            SearchTask singleSearch = new SearchTask(text, pattern, 0, len);
            List<SearchTask> taskList = new ArrayList<SearchTask>();
//            int ntasksRefined = 8;
            int remainder = len % ntasks;
            int cutOf = len / ntasks;
            int newCufOf = cutOf;
            int startIdx = 0;


            if (remainder != 0) {
                for (int x = 0; x < ntasks - 1; x++) {
                    SearchTask searchTask = new SearchTask(text, pattern, startIdx, newCufOf + pattern.length);
                    taskList.add(searchTask);
                    newCufOf += cutOf;
                    startIdx += cutOf;
                    System.out.print("");
                }
                SearchTask searchTask = new SearchTask(text, pattern, startIdx - pattern.length, newCufOf + remainder);
                taskList.add(searchTask);
            } else {
                for (int x = 0; x < ntasks; x++) {
                    SearchTask searchTask = new SearchTask(text, pattern, startIdx, newCufOf + pattern.length);
                    taskList.add(searchTask);
                    newCufOf += cutOf;
                    startIdx += cutOf;
                    System.out.print("");
                }
            }

            List<Integer> singleResult = null;

            /*
             * Run a couple of times on engine for loading all classes and
             * cache warm-up
             */
            for (int i = 0; i < warmups; i++) {
                engine.submit(singleSearch).get();
            }

            /* Run for time measurement(s) and proper result */
            totalTime = 0.0;

            for (int run = 0; run < runs; run++) {
                start = System.nanoTime();

                singleResult = engine.submit(singleSearch).get();

                time = (double) (System.nanoTime() - start) / 1e9;
                totalTime += time;

//                System.out.print("\nSingle task: ");
//                writeRun(run);
//                writeResult(singleResult);
                writeTime(time);
            }
            List<Integer> checkResSingleLast = singleResult;

            double singleTime = totalTime / runs;
            System.out.print("Single task (avg.): ");
            writeTime(singleTime);
            System.out.print("\n\n");
            System.out.println();


            /**********************************************
             * Run search using multiple tasks
             *********************************************/

//Uncomment for Problem 2+

            // Create list of tasks
//            List<SearchTask> taskList = new ArrayList<SearchTask>();
            // Add tasks to list here

            List<Integer> result = null;

            // Run the tasks a couple of times
            for (int i = 0; i < warmups; i++) {
                engine.invokeAll(taskList);
            }

            totalTime = 0.0;

            for (int run = 0; run < runs; run++) {

                start = System.nanoTime();

                // Submit tasks and await results
                List<Future<List<Integer>>> futures = engine.invokeAll(taskList);

                // Overall result is an ordered list of unique occurrence positions
                result = new LinkedList<Integer>();
                for (int i = 0; i < futures.size(); i++) {
                    Future<List<Integer>> future = futures.get(i);
                    for (Integer v : future.get()) {
                        result.add(v);

                    }
                }
                time = (double) (System.nanoTime() - start) / 1e9;
                totalTime += time;

//                System.out.printf("\nUsing %2d tasks: ", ntasks);
//                writeRun(run);
//                writeResult(result);
                writeTime(time);
            }

            double multiTime = totalTime / runs;
            System.out.printf("Using %2d tasks (avg.): ", ntasks);
            writeTime(multiTime);
            System.out.print("\n\n");
            System.out.println();

            List<Integer> checkResMultiLast = new ArrayList<Integer>();
            for (Integer v: result) {
                if (!checkResMultiLast.contains(v))
                {
                    checkResMultiLast.add(v);
                }
            }

            if (!checkResSingleLast.equals(checkResMultiLast)) {
                System.out.println("\nERROR: lists differ");
            }
            System.out.printf("Average speedup: %1.2f\n\n\n\n", singleTime / multiTime);
            engine.shutdown();

            writeResult(checkResSingleLast);
            System.out.printf("\n\n\n");
            writeResult(checkResMultiLast);

        } catch(Exception e){
            System.out.println("Search: " + e);
        }
    }
}

class SearchTask implements Callable<List<Integer>> {

    char[] text, pattern;
    int from = 0, to = 0; // Searched string: text[from..(to-1)]

    /**
     * Create a task for searching occurrences of 'pattern' in the substring
     * text[from..(to-1)]
     */
    public SearchTask(char[] text, char[] pattern, int from, int to) {
        this.text = text;
        this.pattern = pattern;
        this.from = from;
        this.to = to;
    }

    public List<Integer> call() {
        final int pl = pattern.length;
        List<Integer> result = new LinkedList<Integer>();

        // VERY naive string matching to consume some CPU-cycles
        for (int i = from; i <= to - pl; i++) {
            boolean eq = true;
            for (int j = 0; j < pl; j++) {
                if (text[i + j] != pattern[j])
                    eq = false; // We really should break here
            }
            if (eq)
                result.add(i);
        }

        return result;
    }
}