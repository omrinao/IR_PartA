package Searching;

import de.jungblut.datastructure.KDTree;
import de.jungblut.datastructure.KDTree.VectorDistanceTuple;
import de.jungblut.glove.GloveRandomAccessReader;
import de.jungblut.glove.impl.CachedGloveBinaryRandomAccessReader;
import de.jungblut.glove.impl.GloveBinaryRandomAccessReader;
import de.jungblut.glove.impl.GloveBinaryReader;
import de.jungblut.glove.util.StringVectorPair;
import de.jungblut.math.DoubleVector;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;


public class SemanticAid {

    private GloveRandomAccessReader _reader;
    private final KDTree<String> _tree;
    private final int kNeighbors;


    public SemanticAid(String pathToGloveFilesFolder, int kNeighbors) throws IOException {
        this.kNeighbors = kNeighbors;
        // load GloVe vectors
        Path dir = Paths.get(pathToGloveFilesFolder);

        _reader = new CachedGloveBinaryRandomAccessReader(new GloveBinaryRandomAccessReader(dir), 100L);
        _tree = new KDTree<>();

        try (Stream<StringVectorPair> stream = new GloveBinaryReader().stream(dir)) {
            stream.forEach((pair) -> {
                _tree.add(pair.vector, pair.value);
            });

        }
        _tree.balanceBySort();

    }

    /**
     * finds the best synonyms for the given word according to GloVe
     * @param word the word to find synonyms to
     * @return the nearest semantic neighbors for the given word. If word is null, returns null. If no neighbors were found or word wasn't found, returns an empty list.
     * @throws IOException if an error occurs during retrieval
     */
    public List<String> getSynonyms(String word) throws IOException {
        if(word == null) return null;

        DoubleVector v = _reader.get(word);
        if (v == null) {
            // word does'nt exist in vectors
            return new ArrayList<String>();
        } else {
            List<VectorDistanceTuple<String>> nearestNeighbours = _tree.getNearestNeighbours(v, kNeighbors + 1);

            // sort and remove the one that we searched for
            nearestNeighbours.sort(Collections.reverseOrder());
            if (nearestNeighbours.get(0).getValue().equals(word)) {
                nearestNeighbours.remove(0);
            }

            ArrayList<String> neighborStrings = new ArrayList<>(nearestNeighbours.size());
            for (VectorDistanceTuple<String> tuple : nearestNeighbours) {
                neighborStrings.add(tuple.getValue());
            }
            return neighborStrings;
        }
    }
}
