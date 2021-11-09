import java.util.*;

public class HMMDriver {
    public static void main(String[] args) {
        HMM hmm = new HMM("inputs/texts/brown-train-sentences.txt", "inputs/texts/brown-train-tags.txt");
        try {
            hmm.training();
        } catch (Exception e) {
            System.out.println("failed to build HMM");
//            System.err.println(e);
        }

        // test how well the model tags each sentence from the test file
        try {
            hmm.readTestFiles("inputs/texts/brown-test-sentences.txt", "inputs/texts/brown-test-tags.txt");
        } catch (Exception e) {
            System.out.println("failed to do testing");
//            System.err.println(e);
        }

        // test maps used for testing the viterbi decoding algorithm
        // test HMM is hard-coded from PD7
        hmm.buildTestMaps();
        Map<String, Map<String, Double>> t = hmm.getTransitionsTest();
        Map<String, Map<String, Double>> o = hmm.getObservationsTest();

        // HMM of training data
        Map<String, Map<String, Double>> transitions = hmm.getTransitions();
        Map<String, Map<String, Double>> observations = hmm.getObservations();

        // Write a console-based test method to give the tags from an input line.
        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.println("Write a sentence for HMM to tag:");
            String line = in.nextLine();
            System.out.println(hmm.tagInputSentence(line, transitions, observations));
            System.out.println();
        }
    }
}
