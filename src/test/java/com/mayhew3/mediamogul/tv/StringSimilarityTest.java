package com.mayhew3.mediamogul.tv;

import info.debatty.java.stringsimilarity.NGram;
import info.debatty.java.stringsimilarity.interfaces.StringDistance;
import org.junit.Test;

public class StringSimilarityTest {

  @Test
  public void testNGram() {
    StringDistance levenshtein = new NGram();

    System.out.println(levenshtein.distance("IX", "IX"));
    System.out.println(levenshtein.distance("IX", "IX."));
    System.out.println(levenshtein.distance("IX", "XX"));
    System.out.println(levenshtein.distance("IX", "XX."));
    System.out.println(levenshtein.distance("Star Wars", "Star Wars: The Next Generation"));
    System.out.println(levenshtein.distance("Star Wars", "Flippy Fuhrmat and the Peace Patrol"));
    System.out.println(levenshtein.distance("Star Wars The Next Generation", "Star Wars: The Next Generation"));
  }
}
