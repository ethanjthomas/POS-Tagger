import java.util.*;
import java.io.*;

/**
 * Class to build a HMM for tagging parts of speech from text files
 * @author Ethan Thomas, Dartmouth CS 10 Spring 2021, worked with Arnav Tolat
 * @author Arnav Tolat
 */
public class HMM {
    // names for input files
    private String senFile, tagFile;

    private Map<String, Map<String, Double>> transitions;       // map of transitions between states with occurrences of those transitions
    private Map<String, Map<String, Double>> observations;      // map of tags to the map of observation words with their occurrences

    private Map<String, Map<String, Double>> transitionsTest;
    private Map<String, Map<String, Double>> observationsTest;

    private static final double UNSEEN = -100.0;                // unseen score

    public HMM(String senFile, String tagFile) {
        this.senFile = senFile;
        this.tagFile = tagFile;
    }

    public Map<String, Map<String, Double>> getTransitions() {
        return transitions;
    }

    public Map<String, Map<String, Double>> getObservations() {
        return observations;
    }

    public Map<String, Map<String, Double>> getTransitionsTest() {
        return transitionsTest;
    }

    public Map<String, Map<String, Double>> getObservationsTest() {
        return observationsTest;
    }

    /**
     * hard-coded test graph (same model from PD7)
     */
    public void buildTestMaps() {
        transitionsTest = new HashMap<>();
        observationsTest = new HashMap<>();

        // transitionsTest map
        Map<String, Double> startMap = new HashMap<>();
        Map<String, Double> nMap = new HashMap<>();
        Map<String, Double> npMap = new HashMap<>();
        Map<String, Double> cnjMap = new HashMap<>();
        Map<String, Double> vMap = new HashMap<>();

        startMap.put("NP", 3.0);
        startMap.put("N", 7.0);
        transitionsTest.put("#", startMap);

        nMap.put("CNJ", 2.0);
        nMap.put("V", 8.0);
        transitionsTest.put("N", nMap);

        npMap.put("V", 8.0);
        npMap.put("CNJ", 2.0);
        transitionsTest.put("NP", npMap);

        cnjMap.put("V", 4.0);
        cnjMap.put("N", 4.0);
        cnjMap.put("NP", 2.0);
        transitionsTest.put("CNJ", cnjMap);

        vMap.put("NP", 4.0);
        vMap.put("CNJ", 2.0);
        vMap.put("N", 4.0);
        transitionsTest.put("V", vMap);

        // observationsTest map
        Map<String, Double> nM = new HashMap<>();
        Map<String, Double> npM = new HashMap<>();
        Map<String, Double> cnjM = new HashMap<>();
        Map<String, Double> vM = new HashMap<>();

        nM.put("cat", 4.0);
        nM.put("dog", 4.0);
        nM.put("watch", 2.0);
        observationsTest.put("N", nM);

        npM.put("chase", 10.0);
        observationsTest.put("NP", npM);

        cnjM.put("and", 10.0);
        observationsTest.put("CNJ", cnjM);

        vM.put("get", 1.0);
        vM.put("chase", 3.0);
        vM.put("watch", 6.0);
        observationsTest.put("V", vM);

    }

    /**
     * Fill transitions and observations maps
     */
    public void training() throws Exception {
        // initialize maps
        transitions = new HashMap<>();
        observations = new HashMap<>();

        // read files
        BufferedReader s = new BufferedReader(new FileReader(senFile));
        BufferedReader t = new BufferedReader(new FileReader(tagFile));

        // get first line
        String sLine = s.readLine();
        String tLine = t.readLine();

        // read through entirety of files
        while (sLine != null) {
            String[] sArr = sLine.split(" ");
            String[] tArr = tLine.split(" ");

            // first tag in the sentence
            String firstTag = tArr[0];
            processFirst("#", firstTag, transitions);

            // first word in the sentence
            String firstWord = sArr[0].toLowerCase();
            processFirst(firstTag, firstWord, observations);

            // fill out the rest of the maps
            for (int i = 1; i < tArr.length; i++) {
                // transition map
                String currentStateTag = tArr[i - 1];    // current state
                String transitionStateTag = tArr[i];     // next state reached by a transition
                fillMapHelper(currentStateTag, transitionStateTag, transitions);

                // observations map
                String currentTag = tArr[i];                    // current tag
                String word = sArr[i].toLowerCase();            // current word
                fillMapHelper(currentTag, word, observations);
            }
            // go to the next line
            sLine = s.readLine();
            tLine = t.readLine();
        }
        // close files after reading them
        s.close();
        t.close();

        computeScores(transitions);
        computeScores(observations);
    }

    /**
     * method for computing scores for transitions and observation words on a map
     * @param mp    map
     */
    private void computeScores(Map<String, Map<String, Double>> mp) {
        // look at every key in mp
        for (String key : mp.keySet()) {
            //
            double runningSum = 0;
            for (String token : mp.get(key).keySet()) {
                runningSum += mp.get(key).get(token);
            }

            for (String token : mp.get(key).keySet()) {
                mp.get(key).put(token, Math.log(mp.get(key).get(token) / runningSum));
            }
        }
    }

    /**
     * Helper method to process the first transition (from start '#' to first tag) or first tag-word pair
     * @param tag       tag : key for inner map
     * @param other     transition tag / word : key for inner map
     * @param mp        map
     */
    private void processFirst(String tag, String other, Map<String, Map<String, Double>> mp) {
        // if we haven't seen this tag before, introduce it with pointing to a map initialized with the first transition state tag / word with count of 1
        if (mp.get(tag) == null) {
            Map<String, Double> pointToMap = new HashMap<>();
            pointToMap.put(other, 1.0);
            mp.put(tag, pointToMap);
        }
        // otherwise we have seen this tag before
        else {
            // if we have seen the transition state tag / word before
            if (mp.get(tag).containsKey(other)) {
                // increment the occurrence count
                mp.get(tag).put(other, mp.get(tag).get(other) + 1);
            }
            // otherwise introduce this new transition state tag / word
            else {
                mp.get(tag).put(other, 1.0);
            }
        }
    }

    /**
     * Helper method to fill in the map after processing the tags in the sentence
     * @param tag       tag : key for outer map
     * @param other     transition tag / word : key for inner map
     * @param mp        map
     */
    private void fillMapHelper(String tag, String other, Map<String, Map<String, Double>> mp) {
        // if we have seen this tag before
        if (mp.containsKey(tag)) {
            // if we have already recorded the transition tag / word
            if (mp.get(tag).containsKey(other)) {
                // increment the occurrence
                mp.get(tag).put(other, mp.get(tag).get(other) + 1);
            }
            // otherwise we haven't seen this transition tag / word for this tag before
            else {
                mp.get(tag).put(other, 1.0);
            }
        }
        // otherwise we haven't seen this tag before
        else {
            Map<String, Double> pointToMap = new HashMap<>();
            pointToMap.put(other, 1.0);
            mp.put(tag, pointToMap);
        }
    }

    /**
     * method to perform the viterbi algorithm on a sentence to predict the POS of each word in the sentence
     * @param sentence  sentence to be tagged
     * @return          sentence of POS tags
     */
    public String viterbi(String sentence, Map<String, Map<String, Double>> transitionsMap, Map<String, Map<String, Double>> observationsMap) {
        String[] senArr = sentence.split(" ");

        // set of current states at observation i
        Set<String> currStates = new HashSet<>();
        // initialize with special case state of "#"
        currStates.add("#");

        // map to store scores of certain states
        Map<String, Double> currScores = new HashMap<>();
        // initialize with special case state of "#" and score 0.0
        currScores.put("#", 0.0);

        // list to store the mapping of backtrack maps (next state -> current state)
        List<Map<String, String>> pointerList = new ArrayList<>();

        // loop over sentence
        for (int i = 0; i < senArr.length; i++) {
            // word at position i
            String word = senArr[i].toLowerCase();

            // set of next states
            Set<String> nextStates = new HashSet<>();

            // map of next states -> next scores
            Map<String, Double> nextScores = new HashMap<>();

            Iterator<String> it = currStates.iterator();

            // map of backtrack pointers for each observation
            Map<String, String> pointerMap = new HashMap<>();

            // look at each current state
            while (it.hasNext()) {
                String currState = it.next();

                // as long as we have a next state we can go to from the current state
                if (transitionsMap.get(currState) != null) {
                    // look through each next possible state
                    for (String nextState : transitionsMap.get(currState).keySet()) {
                        // record that we've visited the next state
                        nextStates.add(nextState);

                        // get the previous score
                        Double prevScore = currScores.get(currState);
                        // transition score from current state to next state
                        Double transit = transitionsMap.get(currState).get(nextState);
                        // observation score; set to unseen (-100.0) by default
                        Double obs = UNSEEN;

                        // update obs to the observation score if this is a word we've seen before
                        if (observationsMap.get(nextState).containsKey(word)) {
                            obs = observationsMap.get(nextState).get(word);
                        }

                        // compute next score
                        Double nextScore = prevScore + transit + obs;

                        // record the backtrack : next state -> current state
                        if (nextScores.get(nextState) == null) {
                            nextScores.put(nextState, nextScore);
                            // remember that pred of nextState @ i is curr
                            pointerMap.put(nextState, currState);
                        } else {
                            if (nextScore > nextScores.get(nextState)) {
                                nextScores.put(nextState, nextScore);
                                // remember that pred of nextState @ i is curr
                                pointerMap.put(nextState, currState);
                            }
                        }
                    }
                }
            }
            // add the backtrack pointer maps to the list
            pointerList.add(pointerMap);

            // update the data structures to go to the next states
            currStates = nextStates;
            currScores = nextScores;
        }

        // arbitrarily small number; used to find the best score from the last set of possible states in the sentence
        Double bestScore = -10000000000000.0;
        // label for the tag with the best score
        String bestTag = "";

        // determine the tag with the best score
        for (String state : currScores.keySet()) {
            if(currScores.get(state) > bestScore) {
                bestTag = state;
                bestScore = currScores.get(state);
            }
        }

        // label for the current tag in backtracking
        String tag = bestTag;
        // stack to build the tag sentence after backtracking
        Stack<String> tags = new Stack<>();

        // backtrack
        for (int i = pointerList.size() - 1; i >= 0; i--) {
            Map<String, String> m = pointerList.get(i);
            tags.push(tag);
            tag = m.get(tag);
        }

        // sentence of POS tags representing the initial sentence
        String res = "";

        // construct the tagged sentence
        while (!tags.isEmpty()) {
            res += tags.pop() + " ";
        }

        res = res.substring(0, res.length() - 1);

        return res;
    }

    /**
     * method to read test files to determine how well viterbi tagged each sentence in the sentences test file
     * @param testSentences sentences file
     * @param testTags      tags file
     */
    public void readTestFiles(String testSentences, String testTags) throws Exception {
        BufferedReader sentences = new BufferedReader(new FileReader(testSentences));
        BufferedReader tags = new BufferedReader(new FileReader(testTags));

        String sLine = sentences.readLine();
        String tLine = tags.readLine();

        List<String> sentencesLines = new ArrayList<>();
        List<String> tagsLines = new ArrayList<>();

        // add each line in each file to the appropriate list
        while (sLine != null) {
            sentencesLines.add(sLine);
            tagsLines.add(tLine);

            sLine = sentences.readLine();
            tLine = tags.readLine();
        }
        sentences.close();
        tags.close();

        List<String> vitList = new ArrayList<>();

        // perform viterbi on each line in the sentences file
        for (String line : sentencesLines) {
            vitList.add(viterbi(line, transitions, observations));
        }

        int right = 0;
        int wrong = 0;

        // compute how many tags are right/wrong
        for (int i = 0; i < vitList.size(); i++) {
            String[] vitWords = vitList.get(i).split(" ");
            String[] tagWords = tagsLines.get(i).split(" ");

            for (int j = 0; j < vitWords.length; j++) {
                if (!vitWords[j].toLowerCase().equals(tagWords[j].toLowerCase())) {
                    wrong++;
                }
                else {
                    right++;
                }
            }
        }

        System.out.println("Number of tags correct: " + right + "\tNumber of tags incorrect: " + wrong);
    }

    /**
     * method to tag a sentence input by the user
     * @param input sentence from user
     * @return      tagged sentence
     */
    public String tagInputSentence(String input, Map<String, Map<String, Double>> transitionsMap, Map<String, Map<String, Double>> observationsMap) {
        return viterbi(input, transitionsMap, observationsMap);
    }

}
