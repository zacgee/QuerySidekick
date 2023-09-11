/*
* Authors (group members): Jordan Chelsey, Zachary Geelalsingh, Madison Lanahan, Alex Laureano
* Email Addresses of Group Members: jchesley2022@my.fit.edu, zgeelalsingh2022@my.fit.edu, mlanahan2020@my.fit.edu, laureanoedward2003@gmail.com
*
* Course: CSE 2010
* Section: 1234 (combined)
*
* Description of the overall algorithm: Algorithm uses a weighted Trie data structure to store previous queries based on prefixes
*  and makes 5 guesses based on the letters entered.
*  Inspired by: 
*  1. https://www.geeksforgeeks.org/weighted-prefix-search/
*  2. https://www.geeksforgeeks.org/python-bigram-formation-from-given-list/
*  3. Data Structures and Algorithms in Java - Goodrich, Tamassia, Goldwasser.
*/




// Import required packages
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.*;




// Define the QuerySidekick class
public class QuerySidekick {
   private TrieNode trie;
   private Set<String> previousGuesses = new HashSet<>();
   private StringBuilder partialQuery = new StringBuilder();
   // Create a HashMap to store the bigram model
   private Map<String, Map<String, Double>> bigram = new HashMap<>();


   // Constructor for the QuerySidekick class
   public QuerySidekick() {
       trie = new TrieNode();


   }




   // Method to process old queries
   public void processOldQueries(String oldFileName) {
       String line = "";


       // Read the old queries file
       try {
           BufferedReader br = new BufferedReader(new FileReader(oldFileName));


           // Process each line in the file
           while ((line = br.readLine()) != null) {
                addQueryToTrie(line);
           }

           br.close();
       } catch (Exception e) {
           System.out.println(e.getMessage());
           System.out.println(line);
       }
   }




   // Method to guess the user's query
   public String[] guess(char currChar, int currCharPosition) {
       String[] guesses = new String[5];
       Arrays.fill(guesses, null);


       // If it's the first character of the query, reset the partialQuery
       if (currCharPosition == 0) {
           partialQuery = new StringBuilder();
       }
       partialQuery.append(currChar);


       TrieNode node = trie;


       String[] queryWords = partialQuery.toString().split(" ");
       String lastWord = queryWords[queryWords.length - 1];


       // Traverse the Trie to find the node corresponding to the last word
       for (int i = 0; i < lastWord.length(); i++) {
           if (node != null) {
               char key = lastWord.charAt(i);
               node = node.children.get(key);
           } else {
               break;
           }
       }


       // Use a priority queue to store the top guesses based on weight
       PriorityQueue<GuessNode> guessQueue = new PriorityQueue<>(5, (a, b) -> b.weight - a.weight);


       int depthLimit = calculateDepthLimit(lastWord);
       findTopGuesses(node, lastWord, guessQueue, depthLimit);


       String prefix = partialQuery.substring(0, partialQuery.length() - lastWord.length());


       // Fill the guesses array with the top guesses from the Trie
       for (int i = 0; i < 5 && !guessQueue.isEmpty(); i++) {
           String nextGuess = prefix + guessQueue.poll().query;
           while (previousGuesses.contains(nextGuess) && !guessQueue.isEmpty()) {
               nextGuess = prefix + guessQueue.poll().query;
           }
           guesses[i] = nextGuess;
           previousGuesses.add(nextGuess);
       }


       String firstGuessLastWord = "";
       try {
           firstGuessLastWord = guesses[0].substring(prefix.length());
       } catch (NullPointerException e) {
       }


       // If the bigram model contains the first guess last word, use it to refine the guesses
       if (bigram.containsKey(firstGuessLastWord)) {
           Map<String, Double> nextWords = bigram.get(firstGuessLastWord);
           PriorityQueue<Map.Entry<String, Double>> nextWordQueue = new PriorityQueue<>(nextWords.size(), (a, b) -> Double.compare(b.getValue(), a.getValue()));
           nextWordQueue.addAll(nextWords.entrySet());


           for (int i = 1; i < 5; i++) {
               if (!nextWordQueue.isEmpty()) {
                   String nextWord = nextWordQueue.poll().getKey();
                   guesses[i] = guesses[0] + " " + nextWord;
               }
           }
       }


       return guesses;
   }




   // Method to find the top guesses in the Trie based on weight
   private void findTopGuesses(TrieNode node, String query, PriorityQueue<GuessNode> guessQueue, int depth) {
       if (node == null || depth < 0) {
           return;
       }


       // Use a queue to perform a breadth-first search (BFS) on the Trie
       Queue<Map.Entry<TrieNode, String>> queue = new LinkedList<>();
       queue.offer(new AbstractMap.SimpleEntry<>(node, query));


       while (!queue.isEmpty() && depth > 0) {
           Map.Entry<TrieNode, String> current = queue.poll();
           TrieNode currentNode = current.getKey();
           String currentQuery = current.getValue();


           if (currentNode.wordEnd) {
               GuessNode newNode = new GuessNode(currentQuery, currentNode.weight);


               // Update the priority queue with the new node based on weight
               if (guessQueue.size() < 5) {
                   guessQueue.offer(newNode);
               } else if (newNode.weight > guessQueue.peek().weight) {
                   guessQueue.poll();
                   guessQueue.offer(newNode);
               }


               depth--;
           }


           // Add the child nodes to the queue
           for (Map.Entry<Character, TrieNode> entry : currentNode.children.entrySet()) {
               queue.offer(new AbstractMap.SimpleEntry<>(entry.getValue(), currentQuery + entry.getKey()));
           }
       }
   }


   // Calculate the depth limit for the BFS based on the input length
   private int calculateDepthLimit(String input) {
       int minLength = 3; // Minimum length for which depth is calculated
       int minDepth = 5;  // Minimum depth
       int maxDepth = 20; // Maximum depth
       int inputLength = input.length();


       if (inputLength <= minLength) {
           return minDepth;
       } else {
           int depthIncreasePerChar = (maxDepth - minDepth) / (inputLength - minLength);
           return minDepth + depthIncreasePerChar * (inputLength - minLength);
       }
   }




   // Define the GuessNode inner class
   private static class GuessNode {
       String query;
       int weight;




       public GuessNode(String query, int weight) {
           this.query = query;
           this.weight = weight;
       }
   }




   // Method to provide feedback on the user's guess
   public void feedback(boolean isCorrectGuess, String correctQuery) {
       if (isCorrectGuess) {
           System.out.println("Congratulations! Your guess is correct: " + correctQuery);
           previousGuesses.clear();
       } else if (correctQuery == null) {
           System.out.println("Sorry, none of the guesses is correct.");
       } else {
           System.out.println("Sorry, your guess is incorrect. The correct query is: " + correctQuery);
           addQueryToTrie(correctQuery);
           previousGuesses.clear();
       }
   }

    public void addQueryToTrie(String query) {
        String[] tokens = query.split(" ");
        for(int i = 0; i < tokens.length; i++) {
            String token = tokens[i];


            // Insert the token into the Trie
            trie.insert(token);


            // Build the bigram model
            if(i < tokens.length - 1) {
                String firstWord = tokens[i];
                String secondWord = tokens[i + 1];


                bigram.putIfAbsent(firstWord, new HashMap<>());
                bigram.get(firstWord).put(secondWord, bigram.get(firstWord).getOrDefault(secondWord, 0.0) + 1.0);
            }
        }
    }




   // Define the TrieNode inner class
   private static class TrieNode {
       private Map<Character, TrieNode> children;
       private boolean wordEnd;
       private int weight;


       public TrieNode() {
           children = new LinkedHashMap<>();
           wordEnd = false;
           weight = 0;
       }


       // Method to insert a word into the Trie
       public void insert(String word) {
           TrieNode node = this;


           for (char letter : word.toCharArray()) {

               if (!node.children.containsKey(letter)) {
                   node.children.put(letter, new TrieNode());
               }


               node.children.get(letter).weight++;
               node = node.children.get(letter);
           }
           node.wordEnd = true;
       }


       public int getWeight() {
           return weight;
       }
   }
}


